/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.smassarn.textsecuregcm.controllers.RetryLaterException;

@Provider
public class RetryLaterExceptionMapper implements ExceptionMapper<RetryLaterException> {
  @Override
  public Response toResponse(RetryLaterException e) {
    return Response.status(413)
                   .header("Retry-After", e.getBackoffDuration().toSeconds())
                   .build();
  }
}

