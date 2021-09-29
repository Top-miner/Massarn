/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostnameUtil {

  private static final Logger log = LoggerFactory.getLogger(HostnameUtil.class);

  public static String getLocalHostname() {
    try {
      return InetAddress.getLocalHost().getHostName().toLowerCase(Locale.US);
    } catch (final UnknownHostException e) {
      log.warn("Failed to get hostname", e);
      return "unknown";
    }
  }
}
