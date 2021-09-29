/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.workers;

import static com.codahale.metrics.MetricRegistry.name;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.Metrics;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smassarn.textsecuregcm.WhisperServerConfiguration;
import org.smassarn.textsecuregcm.auth.ExternalServiceCredentialGenerator;
import org.smassarn.textsecuregcm.metrics.PushLatencyManager;
import org.smassarn.textsecuregcm.redis.FaultTolerantRedisCluster;
import org.smassarn.textsecuregcm.securebackup.SecureBackupClient;
import org.smassarn.textsecuregcm.securestorage.SecureStorageClient;
import org.smassarn.textsecuregcm.sqs.DirectoryQueue;
import org.smassarn.textsecuregcm.storage.Account;
import org.smassarn.textsecuregcm.storage.Accounts;
import org.smassarn.textsecuregcm.storage.AccountsManager;
import org.smassarn.textsecuregcm.storage.AccountsManager.DeletionReason;
import org.smassarn.textsecuregcm.storage.DeletedAccounts;
import org.smassarn.textsecuregcm.storage.DeletedAccountsManager;
import org.smassarn.textsecuregcm.storage.DynamicConfigurationManager;
import org.smassarn.textsecuregcm.storage.FaultTolerantDatabase;
import org.smassarn.textsecuregcm.storage.KeysDynamoDb;
import org.smassarn.textsecuregcm.storage.MessagesCache;
import org.smassarn.textsecuregcm.storage.MessagesDynamoDb;
import org.smassarn.textsecuregcm.storage.MessagesManager;
import org.smassarn.textsecuregcm.storage.Profiles;
import org.smassarn.textsecuregcm.storage.ProfilesManager;
import org.smassarn.textsecuregcm.storage.ReportMessageDynamoDb;
import org.smassarn.textsecuregcm.storage.ReportMessageManager;
import org.smassarn.textsecuregcm.storage.ReservedUsernames;
import org.smassarn.textsecuregcm.storage.StoredVerificationCodeManager;
import org.smassarn.textsecuregcm.storage.Usernames;
import org.smassarn.textsecuregcm.storage.UsernamesManager;
import org.smassarn.textsecuregcm.storage.VerificationCodeStore;
import org.smassarn.textsecuregcm.util.DynamoDbFromConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DeleteUserCommand extends EnvironmentCommand<WhisperServerConfiguration> {

  private final Logger logger = LoggerFactory.getLogger(DeleteUserCommand.class);

  public DeleteUserCommand() {
    super(new Application<>() {
      @Override
      public void run(WhisperServerConfiguration configuration, Environment environment) {

      }
    }, "rmuser", "remove user");
  }

  @Override
  public void configure(Subparser subparser) {
    super.configure(subparser);
    subparser.addArgument("-u", "--user")
             .dest("user")
             .type(String.class)
             .required(true)
             .help("The user to remove");
  }

  @Override
  protected void run(Environment environment, Namespace namespace,
                     WhisperServerConfiguration configuration)
      throws Exception
  {
    try {
      String[] users = namespace.getString("user").split(",");

      environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      JdbiFactory           jdbiFactory                 = new JdbiFactory();
      Jdbi                  accountJdbi                 = jdbiFactory.build(environment, configuration.getAccountsDatabaseConfiguration(), "accountdb");
      FaultTolerantDatabase accountDatabase             = new FaultTolerantDatabase("account_database_delete_user", accountJdbi, configuration.getAccountsDatabaseConfiguration().getCircuitBreakerConfiguration());
      ClientResources       redisClusterClientResources = ClientResources.builder().build();

      DynamoDbClient reportMessagesDynamoDb = DynamoDbFromConfig.client(
          configuration.getReportMessageDynamoDbConfiguration(),
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());
      DynamoDbClient messageDynamoDb = DynamoDbFromConfig.client(configuration.getMessageDynamoDbConfiguration(),
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());
      DynamoDbClient preKeysDynamoDb = DynamoDbFromConfig.client(configuration.getKeysDynamoDbConfiguration(),
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());
      DynamoDbClient accountsDynamoDbClient = DynamoDbFromConfig.client(
          configuration.getAccountsDynamoDbConfiguration(),
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());
      DynamoDbClient deletedAccountsDynamoDbClient = DynamoDbFromConfig.client(
          configuration.getDeletedAccountsDynamoDbConfiguration(),
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

      FaultTolerantRedisCluster cacheCluster = new FaultTolerantRedisCluster("main_cache_cluster",
          configuration.getCacheClusterConfiguration(), redisClusterClientResources);

      ExecutorService keyspaceNotificationDispatchExecutor = environment.lifecycle()
          .executorService(name(getClass(), "keyspaceNotification-%d")).maxThreads(4).build();
      ExecutorService backupServiceExecutor = environment.lifecycle()
          .executorService(name(getClass(), "backupService-%d")).maxThreads(8).minThreads(1).build();
      ExecutorService storageServiceExecutor = environment.lifecycle()
          .executorService(name(getClass(), "storageService-%d")).maxThreads(8).minThreads(1).build();

      ExternalServiceCredentialGenerator backupCredentialsGenerator = new ExternalServiceCredentialGenerator(
          configuration.getSecureBackupServiceConfiguration().getUserAuthenticationTokenSharedSecret(), new byte[0],
          false);
      ExternalServiceCredentialGenerator storageCredentialsGenerator = new ExternalServiceCredentialGenerator(
          configuration.getSecureStorageServiceConfiguration().getUserAuthenticationTokenSharedSecret(), new byte[0],
          false);

      DynamicConfigurationManager dynamicConfigurationManager = new DynamicConfigurationManager(
          configuration.getAppConfig().getApplication(), configuration.getAppConfig().getEnvironment(),
          configuration.getAppConfig().getConfigurationName());
      dynamicConfigurationManager.start();

      DynamoDbClient pendingAccountsDynamoDbClient = DynamoDbFromConfig.client(configuration.getPendingAccountsDynamoDbConfiguration(),
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

      AmazonDynamoDB deletedAccountsLockDynamoDbClient = AmazonDynamoDBClientBuilder.standard()
          .withRegion(configuration.getDeletedAccountsLockDynamoDbConfiguration().getRegion())
          .withClientConfiguration(new ClientConfiguration().withClientExecutionTimeout(
                  ((int) configuration.getDeletedAccountsLockDynamoDbConfiguration().getClientExecutionTimeout()
                      .toMillis()))
              .withRequestTimeout(
                  (int) configuration.getDeletedAccountsLockDynamoDbConfiguration().getClientRequestTimeout()
                      .toMillis()))
          .withCredentials(InstanceProfileCredentialsProvider.getInstance())
          .build();

      DeletedAccounts deletedAccounts = new DeletedAccounts(deletedAccountsDynamoDbClient,
          configuration.getDeletedAccountsDynamoDbConfiguration().getTableName(),
          configuration.getDeletedAccountsDynamoDbConfiguration().getNeedsReconciliationIndexName());
      VerificationCodeStore pendingAccounts = new VerificationCodeStore(pendingAccountsDynamoDbClient,
          configuration.getPendingAccountsDynamoDbConfiguration().getTableName());

      Accounts accounts = new Accounts(accountsDynamoDbClient,
          configuration.getAccountsDynamoDbConfiguration().getTableName(),
          configuration.getAccountsDynamoDbConfiguration().getPhoneNumberTableName(),
          configuration.getAccountsDynamoDbConfiguration().getScanPageSize());
      Usernames usernames = new Usernames(accountDatabase);
      Profiles profiles = new Profiles(accountDatabase);
      ReservedUsernames reservedUsernames = new ReservedUsernames(accountDatabase);
      KeysDynamoDb keysDynamoDb = new KeysDynamoDb(preKeysDynamoDb,
          configuration.getKeysDynamoDbConfiguration().getTableName());
      MessagesDynamoDb messagesDynamoDb = new MessagesDynamoDb(messageDynamoDb,
          configuration.getMessageDynamoDbConfiguration().getTableName(),
          configuration.getMessageDynamoDbConfiguration().getTimeToLive());
      FaultTolerantRedisCluster messageInsertCacheCluster = new FaultTolerantRedisCluster("message_insert_cluster",
          configuration.getMessageCacheConfiguration().getRedisClusterConfiguration(), redisClusterClientResources);
      FaultTolerantRedisCluster messageReadDeleteCluster = new FaultTolerantRedisCluster("message_read_delete_cluster",
          configuration.getMessageCacheConfiguration().getRedisClusterConfiguration(), redisClusterClientResources);
      FaultTolerantRedisCluster metricsCluster = new FaultTolerantRedisCluster("metrics_cluster",
          configuration.getMetricsClusterConfiguration(), redisClusterClientResources);
      SecureBackupClient secureBackupClient = new SecureBackupClient(backupCredentialsGenerator, backupServiceExecutor,
          configuration.getSecureBackupServiceConfiguration());
      SecureStorageClient secureStorageClient = new SecureStorageClient(storageCredentialsGenerator,
          storageServiceExecutor, configuration.getSecureStorageServiceConfiguration());
      MessagesCache messagesCache = new MessagesCache(messageInsertCacheCluster, messageReadDeleteCluster,
          keyspaceNotificationDispatchExecutor);
      PushLatencyManager pushLatencyManager = new PushLatencyManager(metricsCluster);
      DirectoryQueue directoryQueue = new DirectoryQueue(
          configuration.getDirectoryConfiguration().getSqsConfiguration());
      UsernamesManager usernamesManager = new UsernamesManager(usernames, reservedUsernames, cacheCluster);
      ProfilesManager profilesManager = new ProfilesManager(profiles, cacheCluster);
      ReportMessageDynamoDb reportMessageDynamoDb = new ReportMessageDynamoDb(reportMessagesDynamoDb,
          configuration.getReportMessageDynamoDbConfiguration().getTableName());
      ReportMessageManager reportMessageManager = new ReportMessageManager(reportMessageDynamoDb,
          Metrics.globalRegistry);
      MessagesManager messagesManager = new MessagesManager(messagesDynamoDb, messagesCache, pushLatencyManager,
          reportMessageManager);
      DeletedAccountsManager deletedAccountsManager = new DeletedAccountsManager(deletedAccounts,
          deletedAccountsLockDynamoDbClient,
          configuration.getDeletedAccountsLockDynamoDbConfiguration().getTableName());
      StoredVerificationCodeManager pendingAccountsManager = new StoredVerificationCodeManager(pendingAccounts);
      AccountsManager accountsManager = new AccountsManager(accounts, cacheCluster,
          deletedAccountsManager, directoryQueue, keysDynamoDb, messagesManager, usernamesManager, profilesManager,
          pendingAccountsManager, secureStorageClient, secureBackupClient);

      for (String user : users) {
        Optional<Account> account = accountsManager.get(user);

        if (account.isPresent()) {
          accountsManager.delete(account.get(), DeletionReason.ADMIN_DELETED);
          logger.warn("Removed " + account.get().getNumber());
        } else {
          logger.warn("Account not found");
        }
      }
    } catch (Exception ex) {
      logger.warn("Removal Exception", ex);
      throw new RuntimeException(ex);
    }
  }
}
