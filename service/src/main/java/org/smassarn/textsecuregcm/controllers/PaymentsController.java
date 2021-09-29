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
import org.smassarn.textsecuregcm.currency.CurrencyConversionManager;
import org.smassarn.textsecuregcm.entities.CurrencyConversionEntityList;

@Path("/v1/payments")
public class PaymentsController {

  private final ExternalServiceCredentialGenerator paymentsServiceCredentialGenerator;
  private final CurrencyConversionManager          currencyManager;

  public PaymentsController(CurrencyConversionManager currencyManager, ExternalServiceCredentialGenerator paymentsServiceCredentialGenerator) {
    this.currencyManager                    = currencyManager;
    this.paymentsServiceCredentialGenerator = paymentsServiceCredentialGenerator;
  }

  @Timed
  @GET
  @Path("/auth")
  @Produces(MediaType.APPLICATION_JSON)
  public ExternalServiceCredentials getAuth(@Auth AuthenticatedAccount auth) {
    return paymentsServiceCredentialGenerator.generateFor(auth.getAccount().getUuid().toString());
  }

  @Timed
  @GET
  @Path("/conversions")
  @Produces(MediaType.APPLICATION_JSON)
  public CurrencyConversionEntityList getConversions(@Auth AuthenticatedAccount auth) {
    return currencyManager.getCurrencyConversions().orElseThrow();
  }
}
