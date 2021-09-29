/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.websocket.logging.layout.converters;

import org.smassarn.websocket.logging.WebsocketEvent;

public class NAConverter extends WebSocketEventConverter {
  @Override
  public String convert(WebsocketEvent event) {
    return WebsocketEvent.NA;
  }
}
