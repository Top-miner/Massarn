/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm;

public class WhisperServerVersion {

  private static final String VERSION = "${project.version}";

  public static String getServerVersion() {
    return VERSION;
  }
}
