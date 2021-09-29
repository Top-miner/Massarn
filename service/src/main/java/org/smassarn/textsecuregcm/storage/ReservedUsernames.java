/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.storage;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import org.smassarn.textsecuregcm.util.Constants;

public class ReservedUsernames {

  public static final String ID       = "id";
  public static final String UID      = "uuid";
  public static final String USERNAME = "username";

  private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(Constants.METRICS_NAME);
  private final Timer          queryTimer     = metricRegistry.timer(name(ReservedUsernames.class, "query"));

  private final FaultTolerantDatabase database;

  public ReservedUsernames(FaultTolerantDatabase database) {
    this.database = database;
  }

  public boolean isReserved(String username, UUID uuid) {
    return database.with(jdbi -> jdbi.withHandle(handle -> {
      try (Timer.Context ignored = queryTimer.time()) {
        Optional<Integer> reservations = handle.createQuery("SELECT COUNT(*) FROM reserved_usernames WHERE " + UID + " != :uuid AND :username ~* " + USERNAME)
                                               .bind("username", username)
                                               .bind("uuid", uuid)
                                               .mapTo(Integer.class)
                                               .findFirst();

        return reservations.isPresent() && reservations.get() > 0;
      }
    }));
  }

  @VisibleForTesting
  public void setReserved(String username, UUID reservedFor) {
    database.use(jdbi -> jdbi.useHandle(handle -> {
      handle.createUpdate("INSERT INTO reserved_usernames (" + USERNAME + ", " + UID + ") VALUES(:username, :uuid)")
            .bind("username", username)
            .bind("uuid", reservedFor)
            .execute();
    }));
  }

}
