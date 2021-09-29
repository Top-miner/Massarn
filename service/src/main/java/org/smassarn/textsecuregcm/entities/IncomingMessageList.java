/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class IncomingMessageList {

  @JsonProperty
  @NotNull
  @Valid
  private List<@NotNull IncomingMessage> messages;

  @JsonProperty
  private long timestamp;

  @JsonProperty
  private boolean online;

  public IncomingMessageList() {}

  public List<IncomingMessage> getMessages() {
    return messages;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isOnline() {
    return online;
  }
}
