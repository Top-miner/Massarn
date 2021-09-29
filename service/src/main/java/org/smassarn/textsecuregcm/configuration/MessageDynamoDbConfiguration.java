/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.configuration;

import java.time.Duration;
import javax.validation.Valid;

public class MessageDynamoDbConfiguration extends DynamoDbConfiguration {

  private Duration timeToLive = Duration.ofDays(14);

  @Valid
  public Duration getTimeToLive() {
    return timeToLive;
  }
}
