/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountDatabaseCrawlerConfiguration {

  @JsonProperty
  private int chunkSize = 1000;

  @JsonProperty
  private long chunkIntervalMs = 8000L;

  public int getChunkSize() {
    return chunkSize;
  }

  public long getChunkIntervalMs() {
    return chunkIntervalMs;
  }
}
