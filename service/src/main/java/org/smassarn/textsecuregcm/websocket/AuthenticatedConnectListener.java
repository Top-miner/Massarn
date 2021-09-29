/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.websocket;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smassarn.textsecuregcm.auth.AuthenticatedAccount;
import org.smassarn.textsecuregcm.push.ApnFallbackManager;
import org.smassarn.textsecuregcm.push.ClientPresenceManager;
import org.smassarn.textsecuregcm.push.MessageSender;
import org.smassarn.textsecuregcm.push.ReceiptSender;
import org.smassarn.textsecuregcm.redis.RedisOperation;
import org.smassarn.textsecuregcm.storage.Device;
import org.smassarn.textsecuregcm.storage.MessagesManager;
import org.smassarn.textsecuregcm.util.Constants;
import org.smassarn.websocket.session.WebSocketSessionContext;
import org.smassarn.websocket.setup.WebSocketConnectListener;

public class AuthenticatedConnectListener implements WebSocketConnectListener {

  private static final MetricRegistry metricRegistry               = SharedMetricRegistries.getOrCreate(Constants.METRICS_NAME);
  private static final Timer          durationTimer                = metricRegistry.timer(name(WebSocketConnection.class, "connected_duration"                 ));
  private static final Timer          unauthenticatedDurationTimer = metricRegistry.timer(name(WebSocketConnection.class, "unauthenticated_connection_duration"));
  private static final Counter        openWebsocketCounter         = metricRegistry.counter(name(WebSocketConnection.class, "open_websockets"));

  private static final Logger log = LoggerFactory.getLogger(AuthenticatedConnectListener.class);

  private final ReceiptSender         receiptSender;
  private final MessagesManager       messagesManager;
  private final MessageSender         messageSender;
  private final ApnFallbackManager    apnFallbackManager;
  private final ClientPresenceManager clientPresenceManager;
  private final ScheduledExecutorService retrySchedulingExecutor;

  public AuthenticatedConnectListener(ReceiptSender receiptSender,
      MessagesManager messagesManager,
      final MessageSender messageSender, ApnFallbackManager apnFallbackManager,
      ClientPresenceManager clientPresenceManager,
      ScheduledExecutorService retrySchedulingExecutor)
  {
    this.receiptSender         = receiptSender;
    this.messagesManager       = messagesManager;
    this.messageSender         = messageSender;
    this.apnFallbackManager    = apnFallbackManager;
    this.clientPresenceManager = clientPresenceManager;
    this.retrySchedulingExecutor = retrySchedulingExecutor;
  }

  @Override
  public void onWebSocketConnect(WebSocketSessionContext context) {
    if (context.getAuthenticated() != null) {
      final AuthenticatedAccount auth = context.getAuthenticated(AuthenticatedAccount.class);
      final Device device = auth.getAuthenticatedDevice();
      final Timer.Context timer = durationTimer.time();
      final WebSocketConnection connection = new WebSocketConnection(receiptSender,
          messagesManager, auth, device,
          context.getClient(),
          retrySchedulingExecutor);

      openWebsocketCounter.inc();
      RedisOperation.unchecked(() -> apnFallbackManager.cancel(auth.getAccount(), device));

      context.addListener(new WebSocketSessionContext.WebSocketEventListener() {
        @Override
        public void onWebSocketClose(WebSocketSessionContext context, int statusCode, String reason) {
          openWebsocketCounter.dec();
          timer.stop();

          connection.stop();

          RedisOperation.unchecked(
              () -> clientPresenceManager.clearPresence(auth.getAccount().getUuid(), device.getId()));
          RedisOperation.unchecked(() -> {
            messagesManager.removeMessageAvailabilityListener(connection);

            if (messagesManager.hasCachedMessages(auth.getAccount().getUuid(), device.getId())) {
              messageSender.sendNewMessageNotification(auth.getAccount(), device);
            }
          });
        }
      });

      try {
        connection.start();
        clientPresenceManager.setPresent(auth.getAccount().getUuid(), device.getId(), connection);
        messagesManager.addMessageAvailabilityListener(auth.getAccount().getUuid(), device.getId(), connection);
      } catch (final Exception e) {
        log.warn("Failed to initialize websocket", e);
        context.getClient().close(1011, "Unexpected error initializing connection");
      }
    } else {
      final Timer.Context timer = unauthenticatedDurationTimer.time();
      context.addListener((context1, statusCode, reason) -> timer.stop());
    }
  }
}
