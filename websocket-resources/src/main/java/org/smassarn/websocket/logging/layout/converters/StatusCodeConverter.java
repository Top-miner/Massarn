/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.websocket.logging.layout.converters;

import org.smassarn.websocket.logging.WebsocketEvent;

public class StatusCodeConverter extends WebSocketEventConverter {
  @Override
  public String convert(WebsocketEvent event) {
    if (event.getStatusCode() == WebsocketEvent.SENTINEL) {
      return WebsocketEvent.NA;
    } else {
      return Integer.toString(event.getStatusCode());
    }
  }
}
