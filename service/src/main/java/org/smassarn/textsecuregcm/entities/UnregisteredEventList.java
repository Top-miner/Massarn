/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;

public class UnregisteredEventList {

  @JsonProperty
  private List<UnregisteredEvent> devices;

  public List<UnregisteredEvent> getDevices() {
    if (devices == null) return new LinkedList<>();
    else                 return devices;
  }
}
