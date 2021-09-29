/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.smassarn.textsecuregcm.controllers.ServerRejectedException;

public class ServerRejectedExceptionMapper implements ExceptionMapper<ServerRejectedException> {

  @Override
  public Response toResponse(final ServerRejectedException exception) {
    return Response.status(508).build();
  }
}
