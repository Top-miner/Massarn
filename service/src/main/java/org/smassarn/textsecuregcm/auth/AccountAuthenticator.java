/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.auth;

import static com.codahale.metrics.MetricRegistry.name;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.micrometer.core.instrument.Metrics;
import java.util.Optional;
import org.smassarn.textsecuregcm.storage.AccountsManager;

public class AccountAuthenticator extends BaseAccountAuthenticator implements
    Authenticator<BasicCredentials, AuthenticatedAccount> {

  private static final String AUTHENTICATION_COUNTER_NAME = name(AccountAuthenticator.class, "authenticate");

  public AccountAuthenticator(AccountsManager accountsManager) {
    super(accountsManager);
  }

  @Override
  public Optional<AuthenticatedAccount> authenticate(BasicCredentials basicCredentials) {
    final Optional<AuthenticatedAccount> maybeAuthenticatedAccount = super.authenticate(basicCredentials, true);

    // TODO Remove after announcement groups have launched
    maybeAuthenticatedAccount.ifPresent(authenticatedAccount ->
        Metrics.counter(AUTHENTICATION_COUNTER_NAME,
            "supportsAnnouncementGroups",
            String.valueOf(authenticatedAccount.getAccount().isAnnouncementGroupSupported()))
            .increment());

    return maybeAuthenticatedAccount;
  }

}
