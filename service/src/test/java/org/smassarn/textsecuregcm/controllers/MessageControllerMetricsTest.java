/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.smassarn.textsecuregcm.limits.RateLimitChallengeManager;
import org.smassarn.textsecuregcm.limits.RateLimiters;
import org.smassarn.textsecuregcm.limits.UnsealedSenderRateLimiter;
import org.smassarn.textsecuregcm.push.ApnFallbackManager;
import org.smassarn.textsecuregcm.push.MessageSender;
import org.smassarn.textsecuregcm.push.ReceiptSender;
import org.smassarn.textsecuregcm.redis.AbstractRedisClusterTest;
import org.smassarn.textsecuregcm.storage.AccountsManager;
import org.smassarn.textsecuregcm.storage.DynamicConfigurationManager;
import org.smassarn.textsecuregcm.storage.MessagesManager;
import org.smassarn.textsecuregcm.storage.ReportMessageManager;

public class MessageControllerMetricsTest extends AbstractRedisClusterTest {

  private MessageController messageController;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    messageController = new MessageController(
        mock(RateLimiters.class),
        mock(MessageSender.class),
        mock(ReceiptSender.class),
        mock(AccountsManager.class),
        mock(MessagesManager.class),
        mock(UnsealedSenderRateLimiter.class),
        mock(ApnFallbackManager.class),
        mock(DynamicConfigurationManager.class),
        mock(RateLimitChallengeManager.class),
        mock(ReportMessageManager.class),
        getRedisCluster(),
        mock(ScheduledExecutorService.class),
        mock(ExecutorService.class));
  }

  @Test
  public void testRecordInternationalUnsealedSenderMetrics() {
    final String senderIp = "127.0.0.1";

    messageController.recordInternationalUnsealedSenderMetrics(senderIp, "84", "+18005551234");
    messageController.recordInternationalUnsealedSenderMetrics(senderIp, "84", "+18005551234");

    getRedisCluster().useCluster(connection -> {
      assertEquals(1, (long)connection.sync().pfcount(MessageController.getDestinationSetKey(senderIp)));
      assertEquals(2, Long.parseLong(connection.sync().get(MessageController.getMessageCountKey(senderIp)), 10));

      assertTrue(connection.sync().ttl(MessageController.getDestinationSetKey(senderIp)) >= 0);
      assertTrue(connection.sync().ttl(MessageController.getMessageCountKey(senderIp)) >= 0);
    });
  }
}
