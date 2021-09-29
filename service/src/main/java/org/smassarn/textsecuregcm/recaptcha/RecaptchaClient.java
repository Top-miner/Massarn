/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.recaptcha;

public interface RecaptchaClient {
  boolean verify(String token, String ip);
}
