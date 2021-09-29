/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.auth;

import java.util.Optional;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ContainerRequest;
import org.smassarn.textsecuregcm.storage.Account;

class ContainerRequestUtil {

  static Optional<Account> getAuthenticatedAccount(final ContainerRequest request) {
    return Optional.ofNullable(request.getSecurityContext())
        .map(SecurityContext::getUserPrincipal)
        .map(principal -> principal instanceof AccountAndAuthenticatedDeviceHolder
            ? ((AccountAndAuthenticatedDeviceHolder) principal).getAccount() : null);
  }
}
