/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm;

import static com.codahale.metrics.MetricRegistry.name;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jdbi3.strategies.DefaultNameStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.PolymorphicAuthDynamicFeature;
import io.dropwizard.auth.PolymorphicAuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jdbi.v3.core.Jdbi;
import org.massarn.zkgroup.ServerSecretParams;
import org.massarn.zkgroup.auth.ServerZkAuthOperations;
import org.massarn.zkgroup.profiles.ServerZkProfileOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smassarn.dispatch.DispatchManager;
import org.smassarn.textsecuregcm.auth.AccountAuthenticator;
import org.smassarn.textsecuregcm.auth.AuthenticatedAccount;
import org.smassarn.textsecuregcm.auth.CertificateGenerator;
import org.smassarn.textsecuregcm.auth.DisabledPermittedAccountAuthenticator;
import org.smassarn.textsecuregcm.auth.DisabledPermittedAuthenticatedAccount;
import org.smassarn.textsecuregcm.auth.ExternalServiceCredentialGenerator;
import org.smassarn.textsecuregcm.auth.TurnTokenGenerator;
import org.smassarn.textsecuregcm.auth.WebsocketRefreshApplicationEventListener;
import org.smassarn.textsecuregcm.badges.ConfiguredProfileBadgeConverter;
import org.smassarn.textsecuregcm.badges.ProfileBadgeConverter;
import org.smassarn.textsecuregcm.configuration.DirectoryServerConfiguration;
import org.smassarn.textsecuregcm.controllers.AccountController;
import org.smassarn.textsecuregcm.controllers.AttachmentControllerV1;
import org.smassarn.textsecuregcm.controllers.AttachmentControllerV2;
import org.smassarn.textsecuregcm.controllers.AttachmentControllerV3;
import org.smassarn.textsecuregcm.controllers.CertificateController;
import org.smassarn.textsecuregcm.controllers.ChallengeController;
import org.smassarn.textsecuregcm.controllers.DeviceController;
import org.smassarn.textsecuregcm.controllers.DirectoryController;
import org.smassarn.textsecuregcm.controllers.DonationController;
import org.smassarn.textsecuregcm.controllers.KeepAliveController;
import org.smassarn.textsecuregcm.controllers.KeysController;
import org.smassarn.textsecuregcm.controllers.MessageController;
import org.smassarn.textsecuregcm.controllers.PaymentsController;
import org.smassarn.textsecuregcm.controllers.ProfileController;
import org.smassarn.textsecuregcm.controllers.ProvisioningController;
import org.smassarn.textsecuregcm.controllers.RemoteConfigController;
import org.smassarn.textsecuregcm.controllers.SecureBackupController;
import org.smassarn.textsecuregcm.controllers.SecureStorageController;
import org.smassarn.textsecuregcm.controllers.StickerController;
import org.smassarn.textsecuregcm.controllers.VoiceVerificationController;
import org.smassarn.textsecuregcm.currency.CurrencyConversionManager;
import org.smassarn.textsecuregcm.currency.FixerClient;
import org.smassarn.textsecuregcm.currency.FtxClient;
import org.smassarn.textsecuregcm.experiment.ExperimentEnrollmentManager;
import org.smassarn.textsecuregcm.filters.ContentLengthFilter;
import org.smassarn.textsecuregcm.filters.RemoteDeprecationFilter;
import org.smassarn.textsecuregcm.filters.TimestampResponseFilter;
import org.smassarn.textsecuregcm.limits.PreKeyRateLimiter;
import org.smassarn.textsecuregcm.limits.PushChallengeManager;
import org.smassarn.textsecuregcm.limits.RateLimitChallengeManager;
import org.smassarn.textsecuregcm.limits.RateLimitResetMetricsManager;
import org.smassarn.textsecuregcm.limits.RateLimiters;
import org.smassarn.textsecuregcm.limits.UnsealedSenderRateLimiter;
import org.smassarn.textsecuregcm.liquibase.NameableMigrationsBundle;
import org.smassarn.textsecuregcm.mappers.DeviceLimitExceededExceptionMapper;
import org.smassarn.textsecuregcm.mappers.IOExceptionMapper;
import org.smassarn.textsecuregcm.mappers.InvalidWebsocketAddressExceptionMapper;
import org.smassarn.textsecuregcm.mappers.RateLimitChallengeExceptionMapper;
import org.smassarn.textsecuregcm.mappers.RateLimitExceededExceptionMapper;
import org.smassarn.textsecuregcm.mappers.RetryLaterExceptionMapper;
import org.smassarn.textsecuregcm.mappers.ServerRejectedExceptionMapper;
import org.smassarn.textsecuregcm.metrics.ApplicationShutdownMonitor;
import org.smassarn.textsecuregcm.metrics.BufferPoolGauges;
import org.smassarn.textsecuregcm.metrics.CpuUsageGauge;
import org.smassarn.textsecuregcm.metrics.FileDescriptorGauge;
import org.smassarn.textsecuregcm.metrics.FreeMemoryGauge;
import org.smassarn.textsecuregcm.metrics.GarbageCollectionGauges;
import org.smassarn.textsecuregcm.metrics.MaxFileDescriptorGauge;
import org.smassarn.textsecuregcm.metrics.MetricsApplicationEventListener;
import org.smassarn.textsecuregcm.metrics.MetricsRequestEventListener;
import org.smassarn.textsecuregcm.metrics.NetworkReceivedGauge;
import org.smassarn.textsecuregcm.metrics.NetworkSentGauge;
import org.smassarn.textsecuregcm.metrics.OperatingSystemMemoryGauge;
import org.smassarn.textsecuregcm.metrics.PushLatencyManager;
import org.smassarn.textsecuregcm.metrics.TrafficSource;
import org.smassarn.textsecuregcm.providers.MultiRecipientMessageProvider;
import org.smassarn.textsecuregcm.providers.RedisClientFactory;
import org.smassarn.textsecuregcm.providers.RedisClusterHealthCheck;
import org.smassarn.textsecuregcm.push.APNSender;
import org.smassarn.textsecuregcm.push.ApnFallbackManager;
import org.smassarn.textsecuregcm.push.ClientPresenceManager;
import org.smassarn.textsecuregcm.push.GCMSender;
import org.smassarn.textsecuregcm.push.MessageSender;
import org.smassarn.textsecuregcm.push.ProvisioningManager;
import org.smassarn.textsecuregcm.push.ReceiptSender;
import org.smassarn.textsecuregcm.recaptcha.EnterpriseRecaptchaClient;
import org.smassarn.textsecuregcm.recaptcha.LegacyRecaptchaClient;
import org.smassarn.textsecuregcm.recaptcha.TransitionalRecaptchaClient;
import org.smassarn.textsecuregcm.redis.ConnectionEventLogger;
import org.smassarn.textsecuregcm.redis.FaultTolerantRedisCluster;
import org.smassarn.textsecuregcm.redis.ReplicatedJedisPool;
import org.smassarn.textsecuregcm.s3.PolicySigner;
import org.smassarn.textsecuregcm.s3.PostPolicyGenerator;
import org.smassarn.textsecuregcm.securebackup.SecureBackupClient;
import org.smassarn.textsecuregcm.securestorage.SecureStorageClient;
import org.smassarn.textsecuregcm.sms.SmsSender;
import org.smassarn.textsecuregcm.sms.TwilioSmsSender;
import org.smassarn.textsecuregcm.sms.TwilioVerifyExperimentEnrollmentManager;
import org.smassarn.textsecuregcm.sqs.DirectoryQueue;
import org.smassarn.textsecuregcm.storage.AbusiveHostRules;
import org.smassarn.textsecuregcm.storage.AccountCleaner;
import org.smassarn.textsecuregcm.storage.AccountDatabaseCrawler;
import org.smassarn.textsecuregcm.storage.AccountDatabaseCrawlerCache;
import org.smassarn.textsecuregcm.storage.AccountDatabaseCrawlerListener;
import org.smassarn.textsecuregcm.storage.Accounts;
import org.smassarn.textsecuregcm.storage.AccountsManager;
import org.smassarn.textsecuregcm.storage.ContactDiscoveryWriter;
import org.smassarn.textsecuregcm.storage.DeletedAccounts;
import org.smassarn.textsecuregcm.storage.DeletedAccountsDirectoryReconciler;
import org.smassarn.textsecuregcm.storage.DeletedAccountsManager;
import org.smassarn.textsecuregcm.storage.DeletedAccountsTableCrawler;
import org.smassarn.textsecuregcm.storage.DirectoryReconciler;
import org.smassarn.textsecuregcm.storage.DirectoryReconciliationClient;
import org.smassarn.textsecuregcm.storage.DynamicConfigurationManager;
import org.smassarn.textsecuregcm.storage.FaultTolerantDatabase;
import org.smassarn.textsecuregcm.storage.KeysDynamoDb;
import org.smassarn.textsecuregcm.storage.MessagePersister;
import org.smassarn.textsecuregcm.storage.MessagesCache;
import org.smassarn.textsecuregcm.storage.MessagesDynamoDb;
import org.smassarn.textsecuregcm.storage.MessagesManager;
import org.smassarn.textsecuregcm.storage.Profiles;
import org.smassarn.textsecuregcm.storage.ProfilesManager;
import org.smassarn.textsecuregcm.storage.PubSubManager;
import org.smassarn.textsecuregcm.storage.PushChallengeDynamoDb;
import org.smassarn.textsecuregcm.storage.PushFeedbackProcessor;
import org.smassarn.textsecuregcm.storage.RemoteConfigs;
import org.smassarn.textsecuregcm.storage.RemoteConfigsManager;
import org.smassarn.textsecuregcm.storage.ReportMessageDynamoDb;
import org.smassarn.textsecuregcm.storage.ReportMessageManager;
import org.smassarn.textsecuregcm.storage.ReservedUsernames;
import org.smassarn.textsecuregcm.storage.StoredVerificationCodeManager;
import org.smassarn.textsecuregcm.storage.Usernames;
import org.smassarn.textsecuregcm.storage.UsernamesManager;
import org.smassarn.textsecuregcm.storage.VerificationCodeStore;
import org.smassarn.textsecuregcm.util.AsnManager;
import org.smassarn.textsecuregcm.util.Constants;
import org.smassarn.textsecuregcm.util.DynamoDbFromConfig;
import org.smassarn.textsecuregcm.util.HostnameUtil;
import org.smassarn.textsecuregcm.util.TorExitNodeManager;
import org.smassarn.textsecuregcm.util.logging.LoggingUnhandledExceptionMapper;
import org.smassarn.textsecuregcm.util.logging.UncaughtExceptionHandler;
import org.smassarn.textsecuregcm.websocket.AuthenticatedConnectListener;
import org.smassarn.textsecuregcm.websocket.DeadLetterHandler;
import org.smassarn.textsecuregcm.websocket.ProvisioningConnectListener;
import org.smassarn.textsecuregcm.websocket.WebSocketAccountAuthenticator;
import org.smassarn.textsecuregcm.workers.CertificateCommand;
import org.smassarn.textsecuregcm.workers.CheckDynamicConfigurationCommand;
import org.smassarn.textsecuregcm.workers.DeleteUserCommand;
import org.smassarn.textsecuregcm.workers.ServerVersionCommand;
import org.smassarn.textsecuregcm.workers.SetCrawlerAccelerationTask;
import org.smassarn.textsecuregcm.workers.SetRequestLoggingEnabledTask;
import org.smassarn.textsecuregcm.workers.SetUserDiscoverabilityCommand;
import org.smassarn.textsecuregcm.workers.ZkParamsCommand;
import org.smassarn.websocket.WebSocketResourceProviderFactory;
import org.smassarn.websocket.setup.WebSocketEnvironment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

public class WhisperServerService extends Application<WhisperServerConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(WhisperServerService.class);

  @Override
  public void initialize(Bootstrap<WhisperServerConfiguration> bootstrap) {
    bootstrap.addCommand(new DeleteUserCommand());
    bootstrap.addCommand(new CertificateCommand());
    bootstrap.addCommand(new ZkParamsCommand());
    bootstrap.addCommand(new ServerVersionCommand());
    bootstrap.addCommand(new CheckDynamicConfigurationCommand());
    bootstrap.addCommand(new SetUserDiscoverabilityCommand());

    bootstrap.addBundle(new NameableMigrationsBundle<WhisperServerConfiguration>("accountdb", "accountsdb.xml") {
      @Override
      public DataSourceFactory getDataSourceFactory(WhisperServerConfiguration configuration) {
        return configuration.getAccountsDatabaseConfiguration();
      }
    });

    bootstrap.addBundle(new NameableMigrationsBundle<WhisperServerConfiguration>("abusedb", "abusedb.xml") {
      @Override
      public PooledDataSourceFactory getDataSourceFactory(WhisperServerConfiguration configuration) {
        return configuration.getAbuseDatabaseConfiguration();
      }
    });
  }

  @Override
  public String getName() {
    return "whisper-server";
  }

  @Override
  public void run(WhisperServerConfiguration config, Environment environment)
      throws Exception {

    final Clock clock = Clock.systemUTC();

    UncaughtExceptionHandler.register();

    SharedMetricRegistries.add(Constants.METRICS_NAME, environment.metrics());

    final DistributionStatisticConfig defaultDistributionStatisticConfig = DistributionStatisticConfig.builder()
        .percentiles(.75, .95, .99, .999)
        .build();

    {
      final DatadogMeterRegistry datadogMeterRegistry = new DatadogMeterRegistry(
          config.getDatadogConfiguration(), io.micrometer.core.instrument.Clock.SYSTEM);

      datadogMeterRegistry.config().commonTags(
          Tags.of(
              "service", "chat",
              "host", HostnameUtil.getLocalHostname(),
              "version", WhisperServerVersion.getServerVersion(),
              "env", config.getDatadogConfiguration().getEnvironment()))
          .meterFilter(MeterFilter.denyNameStartsWith(MetricsRequestEventListener.REQUEST_COUNTER_NAME))
          .meterFilter(MeterFilter.denyNameStartsWith(MetricsRequestEventListener.ANDROID_REQUEST_COUNTER_NAME))
          .meterFilter(MeterFilter.denyNameStartsWith(MetricsRequestEventListener.DESKTOP_REQUEST_COUNTER_NAME))
          .meterFilter(MeterFilter.denyNameStartsWith(MetricsRequestEventListener.IOS_REQUEST_COUNTER_NAME))
          .meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(final Id id, final DistributionStatisticConfig config) {
              return defaultDistributionStatisticConfig.merge(config);
            }
          });

      Metrics.addRegistry(datadogMeterRegistry);
    }

    environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    environment.getObjectMapper().setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    environment.getObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    ProfileBadgeConverter profileBadgeConverter = new ConfiguredProfileBadgeConverter(
        Clock.systemUTC(), config.getBadges());

    JdbiFactory jdbiFactory = new JdbiFactory(DefaultNameStrategy.CHECK_EMPTY);
    Jdbi        accountJdbi = jdbiFactory.build(environment, config.getAccountsDatabaseConfiguration(), "accountdb");
    Jdbi        abuseJdbi   = jdbiFactory.build(environment, config.getAbuseDatabaseConfiguration(), "abusedb"  );

    FaultTolerantDatabase accountDatabase = new FaultTolerantDatabase("accounts_database", accountJdbi, config.getAccountsDatabaseConfiguration().getCircuitBreakerConfiguration());
    FaultTolerantDatabase abuseDatabase   = new FaultTolerantDatabase("abuse_database", abuseJdbi, config.getAbuseDatabaseConfiguration().getCircuitBreakerConfiguration());

    DynamoDbClient messageDynamoDb = DynamoDbFromConfig.client(config.getMessageDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient preKeyDynamoDb = DynamoDbFromConfig.client(config.getKeysDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient accountsDynamoDbClient = DynamoDbFromConfig.client(config.getAccountsDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient deletedAccountsDynamoDbClient = DynamoDbFromConfig.client(config.getDeletedAccountsDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient pushChallengeDynamoDbClient = DynamoDbFromConfig.client(
        config.getPushChallengeDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient reportMessageDynamoDbClient = DynamoDbFromConfig.client(
        config.getReportMessageDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient pendingAccountsDynamoDbClient = DynamoDbFromConfig.client(
        config.getPendingAccountsDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    DynamoDbClient pendingDevicesDynamoDbClient = DynamoDbFromConfig.client(
        config.getPendingDevicesDynamoDbConfiguration(),
        software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

    AmazonDynamoDB deletedAccountsLockDynamoDbClient = AmazonDynamoDBClientBuilder.standard()
        .withRegion(config.getDeletedAccountsLockDynamoDbConfiguration().getRegion())
        .withClientConfiguration(new ClientConfiguration().withClientExecutionTimeout(
                ((int) config.getDeletedAccountsLockDynamoDbConfiguration().getClientExecutionTimeout().toMillis()))
            .withRequestTimeout(
                (int) config.getDeletedAccountsLockDynamoDbConfiguration().getClientRequestTimeout().toMillis()))
        .withCredentials(InstanceProfileCredentialsProvider.getInstance())
        .build();

    DeletedAccounts deletedAccounts = new DeletedAccounts(deletedAccountsDynamoDbClient,
        config.getDeletedAccountsDynamoDbConfiguration().getTableName(),
        config.getDeletedAccountsDynamoDbConfiguration().getNeedsReconciliationIndexName());

    Accounts accounts = new Accounts(accountsDynamoDbClient,
        config.getAccountsDynamoDbConfiguration().getTableName(),
        config.getAccountsDynamoDbConfiguration().getPhoneNumberTableName(),
        config.getAccountsDynamoDbConfiguration().getScanPageSize());
    Usernames usernames = new Usernames(accountDatabase);
    ReservedUsernames reservedUsernames = new ReservedUsernames(accountDatabase);
    Profiles profiles = new Profiles(accountDatabase);
    KeysDynamoDb keysDynamoDb = new KeysDynamoDb(preKeyDynamoDb, config.getKeysDynamoDbConfiguration().getTableName());
    MessagesDynamoDb messagesDynamoDb = new MessagesDynamoDb(messageDynamoDb,
        config.getMessageDynamoDbConfiguration().getTableName(),
        config.getMessageDynamoDbConfiguration().getTimeToLive());
    AbusiveHostRules abusiveHostRules = new AbusiveHostRules(abuseDatabase);
    RemoteConfigs remoteConfigs = new RemoteConfigs(accountDatabase);
    PushChallengeDynamoDb pushChallengeDynamoDb = new PushChallengeDynamoDb(pushChallengeDynamoDbClient, config.getPushChallengeDynamoDbConfiguration().getTableName());
    ReportMessageDynamoDb reportMessageDynamoDb = new ReportMessageDynamoDb(reportMessageDynamoDbClient, config.getReportMessageDynamoDbConfiguration().getTableName());
    VerificationCodeStore pendingAccounts = new VerificationCodeStore(pendingAccountsDynamoDbClient, config.getPendingAccountsDynamoDbConfiguration().getTableName());
    VerificationCodeStore pendingDevices = new VerificationCodeStore(pendingDevicesDynamoDbClient, config.getPendingDevicesDynamoDbConfiguration().getTableName());

    RedisClientFactory  pubSubClientFactory = new RedisClientFactory("pubsub_cache", config.getPubsubCacheConfiguration().getUrl(), config.getPubsubCacheConfiguration().getReplicaUrls(), config.getPubsubCacheConfiguration().getCircuitBreakerConfiguration());
    ReplicatedJedisPool pubsubClient        = pubSubClientFactory.getRedisClientPool();

    ClientResources generalCacheClientResources       = ClientResources.builder().build();
    ClientResources messageCacheClientResources       = ClientResources.builder().build();
    ClientResources presenceClientResources           = ClientResources.builder().build();
    ClientResources metricsCacheClientResources       = ClientResources.builder().build();
    ClientResources pushSchedulerCacheClientResources = ClientResources.builder().ioThreadPoolSize(4).build();
    ClientResources rateLimitersCacheClientResources =  ClientResources.builder().build();

    ConnectionEventLogger.logConnectionEvents(generalCacheClientResources);
    ConnectionEventLogger.logConnectionEvents(messageCacheClientResources);
    ConnectionEventLogger.logConnectionEvents(presenceClientResources);
    ConnectionEventLogger.logConnectionEvents(metricsCacheClientResources);

    FaultTolerantRedisCluster cacheCluster             = new FaultTolerantRedisCluster("main_cache_cluster", config.getCacheClusterConfiguration(), generalCacheClientResources);
    FaultTolerantRedisCluster messagesCluster          = new FaultTolerantRedisCluster("messages_cluster", config.getMessageCacheConfiguration().getRedisClusterConfiguration(), messageCacheClientResources);
    FaultTolerantRedisCluster clientPresenceCluster    = new FaultTolerantRedisCluster("client_presence_cluster", config.getClientPresenceClusterConfiguration(), presenceClientResources);
    FaultTolerantRedisCluster metricsCluster           = new FaultTolerantRedisCluster("metrics_cluster", config.getMetricsClusterConfiguration(), metricsCacheClientResources);
    FaultTolerantRedisCluster pushSchedulerCluster     = new FaultTolerantRedisCluster("push_scheduler", config.getPushSchedulerCluster(), pushSchedulerCacheClientResources);
    FaultTolerantRedisCluster rateLimitersCluster      = new FaultTolerantRedisCluster("rate_limiters", config.getRateLimitersCluster(), rateLimitersCacheClientResources);

    BlockingQueue<Runnable> keyspaceNotificationDispatchQueue = new ArrayBlockingQueue<>(10_000);
    Metrics.gaugeCollectionSize(name(getClass(), "keyspaceNotificationDispatchQueueSize"), Collections.emptyList(), keyspaceNotificationDispatchQueue);

    ScheduledExecutorService recurringJobExecutor = environment.lifecycle()
        .scheduledExecutorService(name(getClass(), "recurringJob-%d")).threads(6).build();
    ScheduledExecutorService declinedMessageReceiptExecutor = environment.lifecycle()
        .scheduledExecutorService(name(getClass(), "declined-receipt-%d")).threads(2).build();
    ScheduledExecutorService retrySchedulingExecutor              = environment.lifecycle().scheduledExecutorService(name(getClass(), "retry-%d")).threads(2).build();
    ExecutorService          keyspaceNotificationDispatchExecutor = environment.lifecycle().executorService(name(getClass(), "keyspaceNotification-%d")).maxThreads(16).workQueue(keyspaceNotificationDispatchQueue).build();
    ExecutorService          apnSenderExecutor                    = environment.lifecycle().executorService(name(getClass(), "apnSender-%d")).maxThreads(1).minThreads(1).build();
    ExecutorService          gcmSenderExecutor                    = environment.lifecycle().executorService(name(getClass(), "gcmSender-%d")).maxThreads(1).minThreads(1).build();
    ExecutorService          backupServiceExecutor                = environment.lifecycle().executorService(name(getClass(), "backupService-%d")).maxThreads(1).minThreads(1).build();
    ExecutorService          storageServiceExecutor               = environment.lifecycle().executorService(name(getClass(), "storageService-%d")).maxThreads(1).minThreads(1).build();
    ExecutorService          donationExecutor                     = environment.lifecycle().executorService(name(getClass(), "donation-%d")).maxThreads(1).minThreads(1).build();
    ExecutorService multiRecipientMessageExecutor = environment.lifecycle()
        .executorService(name(getClass(), "multiRecipientMessage-%d")).minThreads(64).maxThreads(64).build();

    ExternalServiceCredentialGenerator directoryCredentialsGenerator = new ExternalServiceCredentialGenerator(config.getDirectoryConfiguration().getDirectoryClientConfiguration().getUserAuthenticationTokenSharedSecret(),
            config.getDirectoryConfiguration().getDirectoryClientConfiguration().getUserAuthenticationTokenUserIdSecret(),
            true);

    DynamicConfigurationManager dynamicConfigurationManager = new DynamicConfigurationManager(config.getAppConfig().getApplication(), config.getAppConfig().getEnvironment(), config.getAppConfig().getConfigurationName());
    dynamicConfigurationManager.start();

    ExperimentEnrollmentManager experimentEnrollmentManager = new ExperimentEnrollmentManager(dynamicConfigurationManager);

    TwilioVerifyExperimentEnrollmentManager verifyExperimentEnrollmentManager = new TwilioVerifyExperimentEnrollmentManager(
        config.getVoiceVerificationConfiguration(), experimentEnrollmentManager);

    ExternalServiceCredentialGenerator storageCredentialsGenerator   = new ExternalServiceCredentialGenerator(config.getSecureStorageServiceConfiguration().getUserAuthenticationTokenSharedSecret(), new byte[0], false);
    ExternalServiceCredentialGenerator backupCredentialsGenerator    = new ExternalServiceCredentialGenerator(config.getSecureBackupServiceConfiguration().getUserAuthenticationTokenSharedSecret(), new byte[0], false);
    ExternalServiceCredentialGenerator paymentsCredentialsGenerator  = new ExternalServiceCredentialGenerator(config.getPaymentsServiceConfiguration().getUserAuthenticationTokenSharedSecret(), new byte[0], false);

    SecureBackupClient         secureBackupClient         = new SecureBackupClient(backupCredentialsGenerator, backupServiceExecutor, config.getSecureBackupServiceConfiguration());
    SecureStorageClient        secureStorageClient        = new SecureStorageClient(storageCredentialsGenerator, storageServiceExecutor, config.getSecureStorageServiceConfiguration());
    ClientPresenceManager      clientPresenceManager      = new ClientPresenceManager(clientPresenceCluster, recurringJobExecutor, keyspaceNotificationDispatchExecutor);
    DirectoryQueue             directoryQueue             = new DirectoryQueue(config.getDirectoryConfiguration().getSqsConfiguration());
    StoredVerificationCodeManager pendingAccountsManager  = new StoredVerificationCodeManager(pendingAccounts);
    StoredVerificationCodeManager pendingDevicesManager   = new StoredVerificationCodeManager(pendingDevices);
    UsernamesManager           usernamesManager           = new UsernamesManager(usernames, reservedUsernames, cacheCluster);
    ProfilesManager            profilesManager            = new ProfilesManager(profiles, cacheCluster);
    MessagesCache              messagesCache              = new MessagesCache(messagesCluster, messagesCluster, keyspaceNotificationDispatchExecutor);
    PushLatencyManager         pushLatencyManager         = new PushLatencyManager(metricsCluster);
    ReportMessageManager       reportMessageManager       = new ReportMessageManager(reportMessageDynamoDb, Metrics.globalRegistry);
    MessagesManager            messagesManager            = new MessagesManager(messagesDynamoDb, messagesCache, pushLatencyManager, reportMessageManager);
    DeletedAccountsManager deletedAccountsManager = new DeletedAccountsManager(deletedAccounts,
        deletedAccountsLockDynamoDbClient, config.getDeletedAccountsLockDynamoDbConfiguration().getTableName());
    AccountsManager accountsManager = new AccountsManager(accounts, cacheCluster,
        deletedAccountsManager, directoryQueue, keysDynamoDb, messagesManager, usernamesManager, profilesManager,
        pendingAccountsManager, secureStorageClient, secureBackupClient);
    RemoteConfigsManager remoteConfigsManager = new RemoteConfigsManager(remoteConfigs);
    DeadLetterHandler          deadLetterHandler          = new DeadLetterHandler(accountsManager, messagesManager);
    DispatchManager            dispatchManager            = new DispatchManager(pubSubClientFactory, Optional.of(deadLetterHandler));
    PubSubManager              pubSubManager              = new PubSubManager(pubsubClient, dispatchManager);
    APNSender                  apnSender                  = new APNSender(apnSenderExecutor, accountsManager, config.getApnConfiguration());
    GCMSender                  gcmSender                  = new GCMSender(gcmSenderExecutor, accountsManager, config.getGcmConfiguration().getApiKey());
    RateLimiters               rateLimiters               = new RateLimiters(config.getLimitsConfiguration(), dynamicConfigurationManager, rateLimitersCluster);
    ProvisioningManager        provisioningManager        = new ProvisioningManager(pubSubManager);
    TorExitNodeManager         torExitNodeManager         = new TorExitNodeManager(recurringJobExecutor, config.getTorExitNodeListConfiguration());
    AsnManager                 asnManager                 = new AsnManager(recurringJobExecutor, config.getAsnTableConfiguration());

    AccountAuthenticator                  accountAuthenticator                  = new AccountAuthenticator(accountsManager);
    DisabledPermittedAccountAuthenticator disabledPermittedAccountAuthenticator = new DisabledPermittedAccountAuthenticator(accountsManager);

    RateLimitResetMetricsManager rateLimitResetMetricsManager = new RateLimitResetMetricsManager(metricsCluster, Metrics.globalRegistry);

    UnsealedSenderRateLimiter unsealedSenderRateLimiter = new UnsealedSenderRateLimiter(rateLimiters, rateLimitersCluster, dynamicConfigurationManager, rateLimitResetMetricsManager);
    PreKeyRateLimiter preKeyRateLimiter = new PreKeyRateLimiter(rateLimiters, dynamicConfigurationManager, rateLimitResetMetricsManager);

    ApnFallbackManager       apnFallbackManager = new ApnFallbackManager(pushSchedulerCluster, apnSender, accountsManager);
    TwilioSmsSender          twilioSmsSender    = new TwilioSmsSender(config.getTwilioConfiguration(), dynamicConfigurationManager);
    SmsSender                smsSender          = new SmsSender(twilioSmsSender);
    MessageSender            messageSender      = new MessageSender(apnFallbackManager, clientPresenceManager, messagesManager, gcmSender, apnSender, pushLatencyManager);
    ReceiptSender            receiptSender      = new ReceiptSender(accountsManager, messageSender);
    TurnTokenGenerator       turnTokenGenerator = new TurnTokenGenerator(config.getTurnConfiguration());
    LegacyRecaptchaClient legacyRecaptchaClient = new LegacyRecaptchaClient(config.getRecaptchaConfiguration().getSecret());
    EnterpriseRecaptchaClient enterpriseRecaptchaClient = new EnterpriseRecaptchaClient(
        config.getRecaptchaV2Configuration().getScoreFloor().doubleValue(),
        config.getRecaptchaV2Configuration().getSiteKey(),
        config.getRecaptchaV2Configuration().getProjectPath(),
        config.getRecaptchaV2Configuration().getCredentialConfigurationJson());
    TransitionalRecaptchaClient transitionalRecaptchaClient = new TransitionalRecaptchaClient(legacyRecaptchaClient, enterpriseRecaptchaClient);
    PushChallengeManager     pushChallengeManager = new PushChallengeManager(apnSender, gcmSender, pushChallengeDynamoDb);
    RateLimitChallengeManager rateLimitChallengeManager = new RateLimitChallengeManager(pushChallengeManager,
        transitionalRecaptchaClient, preKeyRateLimiter, unsealedSenderRateLimiter, rateLimiters,
        dynamicConfigurationManager);

    MessagePersister messagePersister = new MessagePersister(messagesCache, messagesManager, accountsManager, dynamicConfigurationManager, Duration.ofMinutes(config.getMessageCacheConfiguration().getPersistDelayMinutes()));

    // TODO listeners must be ordered so that ones that directly update accounts come last, so that read-only ones are not working with stale data
    final List<AccountDatabaseCrawlerListener> accountDatabaseCrawlerListeners = new ArrayList<>();

    final List<DeletedAccountsDirectoryReconciler> deletedAccountsDirectoryReconcilers = new ArrayList<>();
    for (DirectoryServerConfiguration directoryServerConfiguration : config.getDirectoryConfiguration()
        .getDirectoryServerConfiguration()) {
      final DirectoryReconciliationClient directoryReconciliationClient = new DirectoryReconciliationClient(
          directoryServerConfiguration);
      final DirectoryReconciler directoryReconciler = new DirectoryReconciler(
          directoryServerConfiguration.getReplicationName(), directoryReconciliationClient);
      // reconcilers are read-only
      accountDatabaseCrawlerListeners.add(directoryReconciler);

      final DeletedAccountsDirectoryReconciler deletedAccountsDirectoryReconciler = new DeletedAccountsDirectoryReconciler(
          directoryServerConfiguration.getReplicationName(), directoryReconciliationClient);
      deletedAccountsDirectoryReconcilers.add(deletedAccountsDirectoryReconciler);
    }
    accountDatabaseCrawlerListeners.add(new ContactDiscoveryWriter(accountsManager));
    // PushFeedbackProcessor may update device properties
    accountDatabaseCrawlerListeners.add(new PushFeedbackProcessor(accountsManager));
    // delete accounts last
    accountDatabaseCrawlerListeners.add(new AccountCleaner(accountsManager));

    HttpClient                currencyClient  = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(10)).build();
    FixerClient               fixerClient     = new FixerClient(currencyClient, config.getPaymentsServiceConfiguration().getFixerApiKey());
    FtxClient                 ftxClient       = new FtxClient(currencyClient);
    CurrencyConversionManager currencyManager = new CurrencyConversionManager(fixerClient, ftxClient, config.getPaymentsServiceConfiguration().getPaymentCurrencies());

    AccountDatabaseCrawlerCache accountDatabaseCrawlerCache = new AccountDatabaseCrawlerCache(cacheCluster);
    AccountDatabaseCrawler accountDatabaseCrawler = new AccountDatabaseCrawler(accountsManager,
        accountDatabaseCrawlerCache, accountDatabaseCrawlerListeners,
        config.getAccountDatabaseCrawlerConfiguration().getChunkSize(),
        config.getAccountDatabaseCrawlerConfiguration().getChunkIntervalMs()
    );

    DeletedAccountsTableCrawler deletedAccountsTableCrawler = new DeletedAccountsTableCrawler(deletedAccountsManager, deletedAccountsDirectoryReconcilers, cacheCluster, recurringJobExecutor);

    apnSender.setApnFallbackManager(apnFallbackManager);
    environment.lifecycle().manage(new ApplicationShutdownMonitor());
    environment.lifecycle().manage(apnFallbackManager);
    environment.lifecycle().manage(pubSubManager);
    environment.lifecycle().manage(messageSender);
    environment.lifecycle().manage(accountDatabaseCrawler);
    environment.lifecycle().manage(deletedAccountsTableCrawler);
    environment.lifecycle().manage(remoteConfigsManager);
    environment.lifecycle().manage(messagesCache);
    environment.lifecycle().manage(messagePersister);
    environment.lifecycle().manage(clientPresenceManager);
    environment.lifecycle().manage(currencyManager);
    environment.lifecycle().manage(torExitNodeManager);
    environment.lifecycle().manage(asnManager);
    environment.lifecycle().manage(directoryQueue);

    StaticCredentialsProvider cdnCredentialsProvider = StaticCredentialsProvider
        .create(AwsBasicCredentials.create(
            config.getCdnConfiguration().getAccessKey(),
            config.getCdnConfiguration().getAccessSecret()));
    S3Client cdnS3Client               = S3Client.builder()
        .credentialsProvider(cdnCredentialsProvider)
        .region(Region.of(config.getCdnConfiguration().getRegion()))
        .build();
    PostPolicyGenerator profileCdnPolicyGenerator = new PostPolicyGenerator(config.getCdnConfiguration().getRegion(),
        config.getCdnConfiguration().getBucket(), config.getCdnConfiguration().getAccessKey());
    PolicySigner profileCdnPolicySigner = new PolicySigner(config.getCdnConfiguration().getAccessSecret(),
        config.getCdnConfiguration().getRegion());

    ServerSecretParams zkSecretParams = new ServerSecretParams(config.getZkConfig().getServerSecret());
    ServerZkProfileOperations zkProfileOperations = new ServerZkProfileOperations(zkSecretParams);
    ServerZkAuthOperations zkAuthOperations = new ServerZkAuthOperations(zkSecretParams);
    boolean isZkEnabled = config.getZkConfig().isEnabled();

    AuthFilter<BasicCredentials, AuthenticatedAccount> accountAuthFilter = new BasicCredentialAuthFilter.Builder<AuthenticatedAccount>().setAuthenticator(
        accountAuthenticator).buildAuthFilter();
    AuthFilter<BasicCredentials, DisabledPermittedAuthenticatedAccount> disabledPermittedAccountAuthFilter = new BasicCredentialAuthFilter.Builder<DisabledPermittedAuthenticatedAccount>().setAuthenticator(
        disabledPermittedAccountAuthenticator).buildAuthFilter();

    environment.servlets()
        .addFilter("RemoteDeprecationFilter", new RemoteDeprecationFilter(dynamicConfigurationManager))
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");

    environment.jersey().register(new ContentLengthFilter(TrafficSource.HTTP));
    environment.jersey().register(MultiRecipientMessageProvider.class);
    environment.jersey().register(new MetricsApplicationEventListener(TrafficSource.HTTP));
    environment.jersey()
        .register(new PolymorphicAuthDynamicFeature<>(ImmutableMap.of(AuthenticatedAccount.class, accountAuthFilter,
            DisabledPermittedAuthenticatedAccount.class, disabledPermittedAccountAuthFilter)));
    environment.jersey().register(new PolymorphicAuthValueFactoryProvider.Binder<>(
        ImmutableSet.of(AuthenticatedAccount.class, DisabledPermittedAuthenticatedAccount.class)));
    environment.jersey().register(new WebsocketRefreshApplicationEventListener(clientPresenceManager));
    environment.jersey().register(new TimestampResponseFilter());
    environment.jersey().register(new VoiceVerificationController(config.getVoiceVerificationConfiguration().getUrl(),
        config.getVoiceVerificationConfiguration().getLocales()));

    ///
    WebSocketEnvironment<AuthenticatedAccount> webSocketEnvironment = new WebSocketEnvironment<>(environment,
        config.getWebSocketConfiguration(), 90000);
    webSocketEnvironment.setAuthenticator(new WebSocketAccountAuthenticator(accountAuthenticator));
    webSocketEnvironment.setConnectListener(
        new AuthenticatedConnectListener(receiptSender, messagesManager, messageSender, apnFallbackManager,
            clientPresenceManager, retrySchedulingExecutor));
    webSocketEnvironment.jersey().register(new WebsocketRefreshApplicationEventListener(clientPresenceManager));
    webSocketEnvironment.jersey().register(new ContentLengthFilter(TrafficSource.WEBSOCKET));
    webSocketEnvironment.jersey().register(MultiRecipientMessageProvider.class);
    webSocketEnvironment.jersey().register(new MetricsApplicationEventListener(TrafficSource.WEBSOCKET));
    webSocketEnvironment.jersey().register(new KeepAliveController(clientPresenceManager));

    // these should be common, but use @Auth DisabledPermittedAccount, which isn’t supported yet on websocket
    environment.jersey().register(
        new AccountController(pendingAccountsManager, accountsManager, usernamesManager, abusiveHostRules, rateLimiters,
            smsSender, dynamicConfigurationManager, turnTokenGenerator, config.getTestDevices(),
            transitionalRecaptchaClient, gcmSender, apnSender, backupCredentialsGenerator,
            verifyExperimentEnrollmentManager));
    environment.jersey().register(new KeysController(rateLimiters, keysDynamoDb, accountsManager, preKeyRateLimiter, rateLimitChallengeManager));

    final List<Object> commonControllers = List.of(
        new AttachmentControllerV1(rateLimiters, config.getAwsAttachmentsConfiguration().getAccessKey(), config.getAwsAttachmentsConfiguration().getAccessSecret(), config.getAwsAttachmentsConfiguration().getBucket()),
        new AttachmentControllerV2(rateLimiters, config.getAwsAttachmentsConfiguration().getAccessKey(), config.getAwsAttachmentsConfiguration().getAccessSecret(), config.getAwsAttachmentsConfiguration().getRegion(), config.getAwsAttachmentsConfiguration().getBucket()),
        new AttachmentControllerV3(rateLimiters, config.getGcpAttachmentsConfiguration().getDomain(), config.getGcpAttachmentsConfiguration().getEmail(), config.getGcpAttachmentsConfiguration().getMaxSizeInBytes(), config.getGcpAttachmentsConfiguration().getPathPrefix(), config.getGcpAttachmentsConfiguration().getRsaSigningKey()),
        new CertificateController(new CertificateGenerator(config.getDeliveryCertificate().getCertificate(), config.getDeliveryCertificate().getPrivateKey(), config.getDeliveryCertificate().getExpiresDays()), zkAuthOperations, isZkEnabled),
        new ChallengeController(rateLimitChallengeManager),
        new DeviceController(pendingDevicesManager, accountsManager, messagesManager, keysDynamoDb, rateLimiters, config.getMaxDevices()),
        new DirectoryController(directoryCredentialsGenerator),
        new DonationController(donationExecutor, config.getDonationConfiguration()),
        new MessageController(rateLimiters, messageSender, receiptSender, accountsManager, messagesManager, unsealedSenderRateLimiter, apnFallbackManager, dynamicConfigurationManager, rateLimitChallengeManager, reportMessageManager, metricsCluster, declinedMessageReceiptExecutor, multiRecipientMessageExecutor),
        new PaymentsController(currencyManager, paymentsCredentialsGenerator),
        new ProfileController(clock, rateLimiters, accountsManager, profilesManager, usernamesManager, dynamicConfigurationManager, profileBadgeConverter, config.getBadges(), cdnS3Client, profileCdnPolicyGenerator, profileCdnPolicySigner, config.getCdnConfiguration().getBucket(), zkProfileOperations),
        new ProvisioningController(rateLimiters, provisioningManager),
        new RemoteConfigController(remoteConfigsManager, config.getRemoteConfigConfiguration().getAuthorizedTokens(), config.getRemoteConfigConfiguration().getGlobalConfig()),
        new SecureBackupController(backupCredentialsGenerator),
        new SecureStorageController(storageCredentialsGenerator),
        new StickerController(rateLimiters, config.getCdnConfiguration().getAccessKey(),
            config.getCdnConfiguration().getAccessSecret(), config.getCdnConfiguration().getRegion(),
            config.getCdnConfiguration().getBucket())
    );

    for (Object controller : commonControllers) {
      environment.jersey().register(controller);
      webSocketEnvironment.jersey().register(controller);
    }

    WebSocketEnvironment<AuthenticatedAccount> provisioningEnvironment = new WebSocketEnvironment<>(environment,
        webSocketEnvironment.getRequestLog(), 60000);
    provisioningEnvironment.jersey().register(new WebsocketRefreshApplicationEventListener(clientPresenceManager));
    provisioningEnvironment.setConnectListener(new ProvisioningConnectListener(pubSubManager));
    provisioningEnvironment.jersey().register(new MetricsApplicationEventListener(TrafficSource.WEBSOCKET));
    provisioningEnvironment.jersey().register(new KeepAliveController(clientPresenceManager));

    registerCorsFilter(environment);
    registerExceptionMappers(environment, webSocketEnvironment, provisioningEnvironment);

    RateLimitChallengeExceptionMapper rateLimitChallengeExceptionMapper = new RateLimitChallengeExceptionMapper(
        rateLimitChallengeManager);

    environment.jersey().register(rateLimitChallengeExceptionMapper);
    webSocketEnvironment.jersey().register(rateLimitChallengeExceptionMapper);
    provisioningEnvironment.jersey().register(rateLimitChallengeExceptionMapper);

    WebSocketResourceProviderFactory<AuthenticatedAccount> webSocketServlet = new WebSocketResourceProviderFactory<>(
        webSocketEnvironment, AuthenticatedAccount.class, config.getWebSocketConfiguration());
    WebSocketResourceProviderFactory<AuthenticatedAccount> provisioningServlet = new WebSocketResourceProviderFactory<>(
        provisioningEnvironment, AuthenticatedAccount.class, config.getWebSocketConfiguration());

    ServletRegistration.Dynamic websocket = environment.servlets().addServlet("WebSocket", webSocketServlet);
    ServletRegistration.Dynamic provisioning = environment.servlets().addServlet("Provisioning", provisioningServlet);

    websocket.addMapping("/v1/websocket/");
    websocket.setAsyncSupported(true);

    provisioning.addMapping("/v1/websocket/provisioning/");
    provisioning.setAsyncSupported(true);

    environment.admin().addTask(new SetRequestLoggingEnabledTask());
    environment.admin().addTask(new SetCrawlerAccelerationTask(accountDatabaseCrawlerCache));

///

    environment.healthChecks().register("cacheCluster", new RedisClusterHealthCheck(cacheCluster));

    environment.metrics().register(name(CpuUsageGauge.class, "cpu"), new CpuUsageGauge(3, TimeUnit.SECONDS));
    environment.metrics().register(name(FreeMemoryGauge.class, "free_memory"), new FreeMemoryGauge());
    environment.metrics().register(name(NetworkSentGauge.class, "bytes_sent"), new NetworkSentGauge());
    environment.metrics().register(name(NetworkReceivedGauge.class, "bytes_received"), new NetworkReceivedGauge());
    environment.metrics().register(name(FileDescriptorGauge.class, "fd_count"), new FileDescriptorGauge());
    environment.metrics().register(name(MaxFileDescriptorGauge.class, "max_fd_count"), new MaxFileDescriptorGauge());
    environment.metrics()
        .register(name(OperatingSystemMemoryGauge.class, "buffers"), new OperatingSystemMemoryGauge("Buffers"));
    environment.metrics()
        .register(name(OperatingSystemMemoryGauge.class, "cached"), new OperatingSystemMemoryGauge("Cached"));

    BufferPoolGauges.registerMetrics();
    GarbageCollectionGauges.registerMetrics();
  }

  private void registerExceptionMappers(Environment environment,
      WebSocketEnvironment<AuthenticatedAccount> webSocketEnvironment,
      WebSocketEnvironment<AuthenticatedAccount> provisioningEnvironment) {
    environment.jersey().register(new LoggingUnhandledExceptionMapper());
    environment.jersey().register(new IOExceptionMapper());
    environment.jersey().register(new RateLimitExceededExceptionMapper());
    environment.jersey().register(new InvalidWebsocketAddressExceptionMapper());
    environment.jersey().register(new DeviceLimitExceededExceptionMapper());
    environment.jersey().register(new RetryLaterExceptionMapper());
    environment.jersey().register(new ServerRejectedExceptionMapper());

    webSocketEnvironment.jersey().register(new LoggingUnhandledExceptionMapper());
    webSocketEnvironment.jersey().register(new IOExceptionMapper());
    webSocketEnvironment.jersey().register(new RateLimitExceededExceptionMapper());
    webSocketEnvironment.jersey().register(new InvalidWebsocketAddressExceptionMapper());
    webSocketEnvironment.jersey().register(new DeviceLimitExceededExceptionMapper());
    webSocketEnvironment.jersey().register(new RetryLaterExceptionMapper());
    webSocketEnvironment.jersey().register(new ServerRejectedExceptionMapper());

    provisioningEnvironment.jersey().register(new LoggingUnhandledExceptionMapper());
    provisioningEnvironment.jersey().register(new IOExceptionMapper());
    provisioningEnvironment.jersey().register(new RateLimitExceededExceptionMapper());
    provisioningEnvironment.jersey().register(new InvalidWebsocketAddressExceptionMapper());
    provisioningEnvironment.jersey().register(new DeviceLimitExceededExceptionMapper());
    provisioningEnvironment.jersey().register(new RetryLaterExceptionMapper());
    provisioningEnvironment.jersey().register(new ServerRejectedExceptionMapper());
  }

  private void registerCorsFilter(Environment environment) {
    FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    filter.setInitParameter("allowedOrigins", "*");
    filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,X-Massarn-Agent");
    filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS");
    filter.setInitParameter("preflightMaxAge", "5184000");
    filter.setInitParameter("allowCredentials", "true");
  }

  public static void main(String[] args) throws Exception {
    new WhisperServerService().run(args);
  }
}
