/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.configuration;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.net.MalformedURLException;
import java.net.URL;

final class URLDeserializationConverter extends StdConverter<String, URL> {

  @Override
  public URL convert(final String value) {
    try {
      return new URL(value);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
