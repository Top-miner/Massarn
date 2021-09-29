/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.configuration.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Set;

public class DynamicPaymentsConfiguration {

  @JsonProperty
  private Set<String> allowedCountryCodes = Collections.emptySet();

  public Set<String> getAllowedCountryCodes() {
    return allowedCountryCodes;
  }
}
