/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RemoteConfigConfiguration {

  @JsonProperty
  @NotNull
  private List<String> authorizedTokens = new LinkedList<>();

  @NotNull
  @JsonProperty
  private Map<String, String> globalConfig = new HashMap<>();

  public List<String> getAuthorizedTokens() {
    return authorizedTokens;
  }

  public Map<String, String> getGlobalConfig() {
    return globalConfig;
  }
}
