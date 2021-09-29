/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.storage.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.smassarn.textsecuregcm.storage.RemoteConfig;
import org.smassarn.textsecuregcm.storage.RemoteConfigs;


public class RemoteConfigRowMapper implements RowMapper<RemoteConfig> {

  @Override
  public RemoteConfig map(ResultSet rs, StatementContext ctx) throws SQLException {
    return new RemoteConfig(rs.getString(RemoteConfigs.NAME),
                            rs.getInt(RemoteConfigs.PERCENTAGE),
                            new HashSet<>(Arrays.asList((UUID[])rs.getArray(RemoteConfigs.UUIDS).getArray())),
                            rs.getString(RemoteConfigs.DEFAULT_VALUE),
                            rs.getString(RemoteConfigs.VALUE),
                            rs.getString(RemoteConfigs.HASH_KEY));
  }
}
