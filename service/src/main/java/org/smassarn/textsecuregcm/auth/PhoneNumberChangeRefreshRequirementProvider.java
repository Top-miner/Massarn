/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.auth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.glassfish.jersey.server.ContainerRequest;
import org.smassarn.textsecuregcm.storage.Account;
import org.smassarn.textsecuregcm.util.Pair;

public class PhoneNumberChangeRefreshRequirementProvider implements WebsocketRefreshRequirementProvider {

  private static final String INITIAL_NUMBER_KEY =
      PhoneNumberChangeRefreshRequirementProvider.class.getName() + ".initialNumber";

  @Override
  public void handleRequestFiltered(final ContainerRequest request) {
    ContainerRequestUtil.getAuthenticatedAccount(request)
        .ifPresent(account -> request.setProperty(INITIAL_NUMBER_KEY, account.getNumber()));
  }

  @Override
  public List<Pair<UUID, Long>> handleRequestFinished(final ContainerRequest request) {
    final String initialNumber = (String) request.getProperty(INITIAL_NUMBER_KEY);

    if (initialNumber != null) {
      final Optional<Account> maybeAuthenticatedAccount = ContainerRequestUtil.getAuthenticatedAccount(request);

      return maybeAuthenticatedAccount
          .filter(account -> !initialNumber.equals(account.getNumber()))
          .map(account -> account.getDevices().stream()
              .map(device -> new Pair<>(account.getUuid(), device.getId()))
              .collect(Collectors.toList()))
          .orElse(Collections.emptyList());
    } else {
      return Collections.emptyList();
    }
  }
}
