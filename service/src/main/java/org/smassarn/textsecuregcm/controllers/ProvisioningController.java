/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.controllers;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import java.util.Base64;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.smassarn.textsecuregcm.auth.AuthenticatedAccount;
import org.smassarn.textsecuregcm.entities.ProvisioningMessage;
import org.smassarn.textsecuregcm.limits.RateLimiters;
import org.smassarn.textsecuregcm.push.ProvisioningManager;
import org.smassarn.textsecuregcm.websocket.ProvisioningAddress;

@Path("/v1/provisioning")
public class ProvisioningController {

  private final RateLimiters        rateLimiters;
  private final ProvisioningManager provisioningManager;

  public ProvisioningController(RateLimiters rateLimiters, ProvisioningManager provisioningManager) {
    this.rateLimiters        = rateLimiters;
    this.provisioningManager = provisioningManager;
  }

  @Timed
  @Path("/{destination}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void sendProvisioningMessage(@Auth AuthenticatedAccount auth,
      @PathParam("destination") String destinationName,
      @Valid ProvisioningMessage message)
      throws RateLimitExceededException {

    rateLimiters.getMessagesLimiter().validate(auth.getAccount().getUuid());

    if (!provisioningManager.sendProvisioningMessage(new ProvisioningAddress(destinationName, 0),
        Base64.getMimeDecoder().decode(message.getBody()))) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
  }
}
