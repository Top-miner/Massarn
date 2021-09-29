/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.websocket.logging;

import ch.qos.logback.core.AsyncAppenderBase;
import io.dropwizard.logging.async.AsyncAppenderFactory;

public class AsyncWebsocketEventAppenderFactory implements AsyncAppenderFactory<WebsocketEvent> {
  @Override
  public AsyncAppenderBase<WebsocketEvent> build() {
    return new AsyncAppenderBase<WebsocketEvent>() {
      @Override
      protected void preprocess(WebsocketEvent event) {
        event.prepareForDeferredProcessing();
      }
    };
  }
}
