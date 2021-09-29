package org.smassarn.textsecuregcm.tests.limits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smassarn.textsecuregcm.configuration.RateLimitsConfiguration;
import org.smassarn.textsecuregcm.configuration.RateLimitsConfiguration.RateLimitConfiguration;
import org.smassarn.textsecuregcm.configuration.dynamic.DynamicConfiguration;
import org.smassarn.textsecuregcm.configuration.dynamic.DynamicRateLimitsConfiguration;
import org.smassarn.textsecuregcm.limits.CardinalityRateLimiter;
import org.smassarn.textsecuregcm.limits.RateLimiter;
import org.smassarn.textsecuregcm.limits.RateLimiters;
import org.smassarn.textsecuregcm.redis.FaultTolerantRedisCluster;
import org.smassarn.textsecuregcm.storage.DynamicConfigurationManager;

class DynamicRateLimitsTest {

  private DynamicConfigurationManager dynamicConfig;
  private FaultTolerantRedisCluster   redisCluster;

  @BeforeEach
  void setup() {
    this.dynamicConfig = mock(DynamicConfigurationManager.class);
    this.redisCluster  = mock(FaultTolerantRedisCluster.class);

    DynamicConfiguration defaultConfig = new DynamicConfiguration();
    when(dynamicConfig.getConfiguration()).thenReturn(defaultConfig);

  }

  @Test
  void testUnchangingConfiguration() {
    RateLimiters rateLimiters = new RateLimiters(new RateLimitsConfiguration(), dynamicConfig, redisCluster);

    RateLimiter limiter = rateLimiters.getUnsealedIpLimiter();

    assertThat(limiter.getBucketSize()).isEqualTo(dynamicConfig.getConfiguration().getLimits().getUnsealedSenderIp().getBucketSize());
    assertThat(limiter.getLeakRatePerMinute()).isEqualTo(dynamicConfig.getConfiguration().getLimits().getUnsealedSenderIp().getLeakRatePerMinute());
    assertSame(rateLimiters.getUnsealedIpLimiter(), limiter);
  }

  @Test
  void testChangingConfiguration() {
    DynamicConfiguration configuration = mock(DynamicConfiguration.class);
    DynamicRateLimitsConfiguration limitsConfiguration = mock(DynamicRateLimitsConfiguration.class);

    when(configuration.getLimits()).thenReturn(limitsConfiguration);
    when(limitsConfiguration.getUnsealedSenderNumber()).thenReturn(new RateLimitsConfiguration.CardinalityRateLimitConfiguration(10, Duration.ofHours(1)));
    when(limitsConfiguration.getRecaptchaChallengeAttempt()).thenReturn(new RateLimitConfiguration());
    when(limitsConfiguration.getRecaptchaChallengeSuccess()).thenReturn(new RateLimitConfiguration());
    when(limitsConfiguration.getPushChallengeAttempt()).thenReturn(new RateLimitConfiguration());
    when(limitsConfiguration.getPushChallengeSuccess()).thenReturn(new RateLimitConfiguration());
    when(limitsConfiguration.getDailyPreKeys()).thenReturn(new RateLimitConfiguration());

    final RateLimitConfiguration initialRateLimitConfiguration = new RateLimitConfiguration(4, 1.0);
    when(limitsConfiguration.getUnsealedSenderIp()).thenReturn(initialRateLimitConfiguration);
    when(limitsConfiguration.getRateLimitReset()).thenReturn(initialRateLimitConfiguration);

    when(dynamicConfig.getConfiguration()).thenReturn(configuration);

    RateLimiters rateLimiters = new RateLimiters(new RateLimitsConfiguration(), dynamicConfig, redisCluster);

    CardinalityRateLimiter limiter = rateLimiters.getUnsealedSenderCardinalityLimiter();

    assertThat(limiter.getDefaultMaxCardinality()).isEqualTo(10);
    assertThat(limiter.getInitialTtl()).isEqualTo(Duration.ofHours(1));
    assertSame(rateLimiters.getUnsealedSenderCardinalityLimiter(), limiter);

    when(limitsConfiguration.getUnsealedSenderNumber()).thenReturn(new RateLimitsConfiguration.CardinalityRateLimitConfiguration(20, Duration.ofHours(2)));

    CardinalityRateLimiter changed = rateLimiters.getUnsealedSenderCardinalityLimiter();

    assertThat(changed.getDefaultMaxCardinality()).isEqualTo(20);
    assertThat(changed.getInitialTtl()).isEqualTo(Duration.ofHours(2));
    assertNotSame(limiter, changed);
  }

}
