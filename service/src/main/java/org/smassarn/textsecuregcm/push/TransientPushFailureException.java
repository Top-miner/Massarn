/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.push;

public class TransientPushFailureException extends Exception {
  public TransientPushFailureException(String s) {
    super(s);
  }

  public TransientPushFailureException(Exception e) {
    super(e);
  }
}
