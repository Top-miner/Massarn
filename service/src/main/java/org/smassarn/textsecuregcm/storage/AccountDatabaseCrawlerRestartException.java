/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.storage;

public class AccountDatabaseCrawlerRestartException extends Exception {
  public AccountDatabaseCrawlerRestartException(String s) {
    super(s);
  }

  public AccountDatabaseCrawlerRestartException(Exception e) {
    super(e);
  }
}
