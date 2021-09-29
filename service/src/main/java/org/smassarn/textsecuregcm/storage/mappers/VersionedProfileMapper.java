/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.storage.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.smassarn.textsecuregcm.storage.Profiles;
import org.smassarn.textsecuregcm.storage.VersionedProfile;

public class VersionedProfileMapper implements RowMapper<VersionedProfile> {

  @Override
  public VersionedProfile map(ResultSet resultSet, StatementContext ctx) throws SQLException {
    return new VersionedProfile(
        resultSet.getString(Profiles.VERSION),
        resultSet.getString(Profiles.NAME),
        resultSet.getString(Profiles.AVATAR),
        resultSet.getString(Profiles.ABOUT_EMOJI),
        resultSet.getString(Profiles.ABOUT),
        resultSet.getString(Profiles.PAYMENT_ADDRESS),
        resultSet.getBytes(Profiles.COMMITMENT));
  }
}
