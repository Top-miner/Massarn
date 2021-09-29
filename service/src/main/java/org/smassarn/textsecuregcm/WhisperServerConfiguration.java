/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.smassarn.textsecuregcm.configuration.AccountDatabaseCrawlerConfiguration;
import org.smassarn.textsecuregcm.configuration.AccountsDatabaseConfiguration;
import org.smassarn.textsecuregcm.configuration.AccountsDynamoDbConfiguration;
import org.smassarn.textsecuregcm.configuration.ApnConfiguration;
import org.smassarn.textsecuregcm.configuration.AppConfigConfiguration;
import org.smassarn.textsecuregcm.configuration.AwsAttachmentsConfiguration;
import org.smassarn.textsecuregcm.configuration.BadgesConfiguration;
import org.smassarn.textsecuregcm.configuration.CdnConfiguration;
import org.smassarn.textsecuregcm.configuration.DatabaseConfiguration;
import org.smassarn.textsecuregcm.configuration.DatadogConfiguration;
import org.smassarn.textsecuregcm.configuration.DeletedAccountsDynamoDbConfiguration;
import org.smassarn.textsecuregcm.configuration.DirectoryConfiguration;
import org.smassarn.textsecuregcm.configuration.DonationConfiguration;
import org.smassarn.textsecuregcm.configuration.DynamoDbConfiguration;
import org.smassarn.textsecuregcm.configuration.GcmConfiguration;
import org.smassarn.textsecuregcm.configuration.GcpAttachmentsConfiguration;
import org.smassarn.textsecuregcm.configuration.MaxDeviceConfiguration;
import org.smassarn.textsecuregcm.configuration.MessageCacheConfiguration;
import org.smassarn.textsecuregcm.configuration.MessageDynamoDbConfiguration;
import org.smassarn.textsecuregcm.configuration.MonitoredS3ObjectConfiguration;
import org.smassarn.textsecuregcm.configuration.PaymentsServiceConfiguration;
import org.smassarn.textsecuregcm.configuration.PushConfiguration;
import org.smassarn.textsecuregcm.configuration.RateLimitsConfiguration;
import org.smassarn.textsecuregcm.configuration.RecaptchaConfiguration;
import org.smassarn.textsecuregcm.configuration.RecaptchaV2Configuration;
import org.smassarn.textsecuregcm.configuration.RedisClusterConfiguration;
import org.smassarn.textsecuregcm.configuration.RedisConfiguration;
import org.smassarn.textsecuregcm.configuration.RemoteConfigConfiguration;
import org.smassarn.textsecuregcm.configuration.SecureBackupServiceConfiguration;
import org.smassarn.textsecuregcm.configuration.SecureStorageServiceConfiguration;
import org.smassarn.textsecuregcm.configuration.TestDeviceConfiguration;
import org.smassarn.textsecuregcm.configuration.TurnConfiguration;
import org.smassarn.textsecuregcm.configuration.TwilioConfiguration;
import org.smassarn.textsecuregcm.configuration.UnidentifiedDeliveryConfiguration;
import org.smassarn.textsecuregcm.configuration.VoiceVerificationConfiguration;
import org.smassarn.textsecuregcm.configuration.ZkConfig;
import org.smassarn.websocket.configuration.WebSocketConfiguration;

/** @noinspection MismatchedQueryAndUpdateOfCollection, WeakerAccess */
public class WhisperServerConfiguration extends Configuration {

  @NotNull
  @Valid
  @JsonProperty
  private TwilioConfiguration twilio;

  @NotNull
  @Valid
  @JsonProperty
  private PushConfiguration push;

  @NotNull
  @Valid
  @JsonProperty
  private AwsAttachmentsConfiguration awsAttachments;

  @NotNull
  @Valid
  @JsonProperty
  private GcpAttachmentsConfiguration gcpAttachments;

  @NotNull
  @Valid
  @JsonProperty
  private CdnConfiguration cdn;

  @NotNull
  @Valid
  @JsonProperty
  private DatadogConfiguration datadog;

  @NotNull
  @Valid
  @JsonProperty
  private RedisClusterConfiguration cacheCluster;

  @NotNull
  @Valid
  @JsonProperty
  private RedisConfiguration pubsub;

  @NotNull
  @Valid
  @JsonProperty
  private RedisClusterConfiguration metricsCluster;

  @NotNull
  @Valid
  @JsonProperty
  private DirectoryConfiguration directory;

  @NotNull
  @Valid
  @JsonProperty
  private AccountDatabaseCrawlerConfiguration accountDatabaseCrawler;

  @NotNull
  @Valid
  @JsonProperty
  private RedisClusterConfiguration pushSchedulerCluster;

  @NotNull
  @Valid
  @JsonProperty
  private RedisClusterConfiguration rateLimitersCluster;

  @NotNull
  @Valid
  @JsonProperty
  private MessageCacheConfiguration messageCache;

  @NotNull
  @Valid
  @JsonProperty
  private RedisClusterConfiguration clientPresenceCluster;

  @Valid
  @NotNull
  @JsonProperty
  private MessageDynamoDbConfiguration messageDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DynamoDbConfiguration keysDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private AccountsDynamoDbConfiguration accountsDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DeletedAccountsDynamoDbConfiguration deletedAccountsDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DynamoDbConfiguration deletedAccountsLockDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DynamoDbConfiguration pushChallengeDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DynamoDbConfiguration reportMessageDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DynamoDbConfiguration pendingAccountsDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DynamoDbConfiguration pendingDevicesDynamoDb;

  @Valid
  @NotNull
  @JsonProperty
  private DatabaseConfiguration abuseDatabase;

  @Valid
  @NotNull
  @JsonProperty
  private List<TestDeviceConfiguration> testDevices = new LinkedList<>();

  @Valid
  @NotNull
  @JsonProperty
  private List<MaxDeviceConfiguration> maxDevices = new LinkedList<>();

  @Valid
  @NotNull
  @JsonProperty
  private AccountsDatabaseConfiguration accountsDatabase;

  @Valid
  @NotNull
  @JsonProperty
  private RateLimitsConfiguration limits = new RateLimitsConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private WebSocketConfiguration webSocket = new WebSocketConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private TurnConfiguration turn;

  @Valid
  @NotNull
  @JsonProperty
  private GcmConfiguration gcm;

  @Valid
  @NotNull
  @JsonProperty
  private ApnConfiguration apn;

  @Valid
  @NotNull
  @JsonProperty
  private UnidentifiedDeliveryConfiguration unidentifiedDelivery;

  @Valid
  @NotNull
  @JsonProperty
  private VoiceVerificationConfiguration voiceVerification;

  @Valid
  @NotNull
  @JsonProperty
  private RecaptchaConfiguration recaptcha;

  @Valid
  @NotNull
  @JsonProperty
  private RecaptchaV2Configuration recaptchaV2;

  @Valid
  @NotNull
  @JsonProperty
  private SecureStorageServiceConfiguration storageService;

  @Valid
  @NotNull
  @JsonProperty
  private SecureBackupServiceConfiguration backupService;

  @Valid
  @NotNull
  @JsonProperty
  private PaymentsServiceConfiguration paymentsService;

  @Valid
  @NotNull
  @JsonProperty
  private ZkConfig zkConfig;

  @Valid
  @NotNull
  @JsonProperty
  private RemoteConfigConfiguration remoteConfig;

  @Valid
  @NotNull
  @JsonProperty
  private AppConfigConfiguration appConfig;

  @Valid
  @NotNull
  @JsonProperty
  private MonitoredS3ObjectConfiguration torExitNodeList;

  @Valid
  @NotNull
  @JsonProperty
  private MonitoredS3ObjectConfiguration asnTable;

  @Valid
  @NotNull
  @JsonProperty
  private DonationConfiguration donation;

  @Valid
  @NotNull
  @JsonProperty
  private BadgesConfiguration badges;

  private Map<String, String> transparentDataIndex = new HashMap<>();

  public RecaptchaConfiguration getRecaptchaConfiguration() {
    return recaptcha;
  }

  public RecaptchaV2Configuration getRecaptchaV2Configuration() {
    return recaptchaV2;
  }

  public VoiceVerificationConfiguration getVoiceVerificationConfiguration() {
    return voiceVerification;
  }

  public WebSocketConfiguration getWebSocketConfiguration() {
    return webSocket;
  }

  public TwilioConfiguration getTwilioConfiguration() {
    return twilio;
  }

  public PushConfiguration getPushConfiguration() {
    return push;
  }

  public JerseyClientConfiguration getJerseyClientConfiguration() {
    return httpClient;
  }

  public AwsAttachmentsConfiguration getAwsAttachmentsConfiguration() {
    return awsAttachments;
  }

  public GcpAttachmentsConfiguration getGcpAttachmentsConfiguration() {
    return gcpAttachments;
  }

  public RedisClusterConfiguration getCacheClusterConfiguration() {
    return cacheCluster;
  }

  public RedisConfiguration getPubsubCacheConfiguration() {
    return pubsub;
  }

  public RedisClusterConfiguration getMetricsClusterConfiguration() {
    return metricsCluster;
  }

  public DirectoryConfiguration getDirectoryConfiguration() {
    return directory;
  }

  public SecureStorageServiceConfiguration getSecureStorageServiceConfiguration() {
    return storageService;
  }

  public AccountDatabaseCrawlerConfiguration getAccountDatabaseCrawlerConfiguration() {
    return accountDatabaseCrawler;
  }

  public MessageCacheConfiguration getMessageCacheConfiguration() {
    return messageCache;
  }

  public RedisClusterConfiguration getClientPresenceClusterConfiguration() {
    return clientPresenceCluster;
  }

  public RedisClusterConfiguration getPushSchedulerCluster() {
    return pushSchedulerCluster;
  }

  public RedisClusterConfiguration getRateLimitersCluster() {
    return rateLimitersCluster;
  }

  public MessageDynamoDbConfiguration getMessageDynamoDbConfiguration() {
    return messageDynamoDb;
  }

  public DynamoDbConfiguration getKeysDynamoDbConfiguration() {
    return keysDynamoDb;
  }

  public AccountsDynamoDbConfiguration getAccountsDynamoDbConfiguration() {
    return accountsDynamoDb;
  }

  public DeletedAccountsDynamoDbConfiguration getDeletedAccountsDynamoDbConfiguration() {
    return deletedAccountsDynamoDb;
  }

  public DynamoDbConfiguration getDeletedAccountsLockDynamoDbConfiguration() {
    return deletedAccountsLockDynamoDb;
  }

  public DatabaseConfiguration getAbuseDatabaseConfiguration() {
    return abuseDatabase;
  }

  public AccountsDatabaseConfiguration getAccountsDatabaseConfiguration() {
    return accountsDatabase;
  }

  public RateLimitsConfiguration getLimitsConfiguration() {
    return limits;
  }

  public TurnConfiguration getTurnConfiguration() {
    return turn;
  }

  public GcmConfiguration getGcmConfiguration() {
    return gcm;
  }

  public ApnConfiguration getApnConfiguration() {
    return apn;
  }

  public CdnConfiguration getCdnConfiguration() {
    return cdn;
  }

  public DatadogConfiguration getDatadogConfiguration() {
    return datadog;
  }

  public UnidentifiedDeliveryConfiguration getDeliveryCertificate() {
    return unidentifiedDelivery;
  }

  public Map<String, Integer> getTestDevices() {
    Map<String, Integer> results = new HashMap<>();

    for (TestDeviceConfiguration testDeviceConfiguration : testDevices) {
      results.put(testDeviceConfiguration.getNumber(),
                  testDeviceConfiguration.getCode());
    }

    return results;
  }

  public Map<String, Integer> getMaxDevices() {
    Map<String, Integer> results = new HashMap<>();

    for (MaxDeviceConfiguration maxDeviceConfiguration : maxDevices) {
      results.put(maxDeviceConfiguration.getNumber(),
                  maxDeviceConfiguration.getCount());
    }

    return results;
  }

  public Map<String, String> getTransparentDataIndex() {
    return transparentDataIndex;
  }

  public SecureBackupServiceConfiguration getSecureBackupServiceConfiguration() {
    return backupService;
  }

  public PaymentsServiceConfiguration getPaymentsServiceConfiguration() {
    return paymentsService;
  }

  public ZkConfig getZkConfig() {
    return zkConfig;
  }

  public RemoteConfigConfiguration getRemoteConfigConfiguration() {
    return remoteConfig;
  }

  public AppConfigConfiguration getAppConfig() {
    return appConfig;
  }

  public DynamoDbConfiguration getPushChallengeDynamoDbConfiguration() {
    return pushChallengeDynamoDb;
  }

  public DynamoDbConfiguration getReportMessageDynamoDbConfiguration() {
    return reportMessageDynamoDb;
  }

  public DynamoDbConfiguration getPendingAccountsDynamoDbConfiguration() {
    return pendingAccountsDynamoDb;
  }

  public DynamoDbConfiguration getPendingDevicesDynamoDbConfiguration() {
    return pendingDevicesDynamoDb;
  }

  public MonitoredS3ObjectConfiguration getTorExitNodeListConfiguration() {
    return torExitNodeList;
  }

  public MonitoredS3ObjectConfiguration getAsnTableConfiguration() {
    return asnTable;
  }

  public DonationConfiguration getDonationConfiguration() {
    return donation;
  }

  public BadgesConfiguration getBadges() {
    return badges;
  }
}
