/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public class AccountCrawlChunk {

  private final List<Account> accounts;
  @Nullable
  private final UUID lastUuid;

  public AccountCrawlChunk(final List<Account> accounts, @Nullable final UUID lastUuid) {
    this.accounts = accounts;
    this.lastUuid = lastUuid;
  }

  public List<Account> getAccounts() {
    return accounts;
  }

  public Optional<UUID> getLastUuid() {
    return Optional.ofNullable(lastUuid);
  }
}
