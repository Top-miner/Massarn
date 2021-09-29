/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.controllers;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.smassarn.textsecuregcm.auth.AuthenticatedAccount;
import org.smassarn.textsecuregcm.auth.ExternalServiceCredentialGenerator;
import org.smassarn.textsecuregcm.auth.ExternalServiceCredentials;

@Path("/v1/storage")
public class SecureStorageController {

  private final ExternalServiceCredentialGenerator storageServiceCredentialGenerator;

  public SecureStorageController(ExternalServiceCredentialGenerator storageServiceCredentialGenerator) {
    this.storageServiceCredentialGenerator = storageServiceCredentialGenerator;
  }

  @Timed
  @GET
  @Path("/auth")
  @Produces(MediaType.APPLICATION_JSON)
  public ExternalServiceCredentials getAuth(@Auth AuthenticatedAccount auth) {
    return storageServiceCredentialGenerator.generateFor(auth.getAccount().getUuid().toString());
  }
}
