/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class AccountMismatchedDevices {
  @JsonProperty
  public final UUID uuid;

  @JsonProperty
  public final MismatchedDevices devices;

  public AccountMismatchedDevices(final UUID uuid, final MismatchedDevices devices) {
    this.uuid = uuid;
    this.devices = devices;
  }
}
