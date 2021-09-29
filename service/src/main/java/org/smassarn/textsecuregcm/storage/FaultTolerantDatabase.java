/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.storage;

import com.codahale.metrics.SharedMetricRegistries;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jdbi.v3.core.Jdbi;
import org.smassarn.textsecuregcm.configuration.CircuitBreakerConfiguration;
import org.smassarn.textsecuregcm.util.CircuitBreakerUtil;
import org.smassarn.textsecuregcm.util.Constants;

public class FaultTolerantDatabase {

  private final Jdbi           database;
  private final CircuitBreaker circuitBreaker;

  public FaultTolerantDatabase(String name, Jdbi database, CircuitBreakerConfiguration circuitBreakerConfiguration) {
    this.database       = database;
    this.circuitBreaker = CircuitBreaker.of(name, circuitBreakerConfiguration.toCircuitBreakerConfig());

    CircuitBreakerUtil.registerMetrics(SharedMetricRegistries.getOrCreate(Constants.METRICS_NAME),
                                       circuitBreaker,
                                       FaultTolerantDatabase.class);
  }

  public void use(Consumer<Jdbi> consumer) {
    this.circuitBreaker.executeRunnable(() -> consumer.accept(database));
  }

  public <T> T with(Function<Jdbi, T> consumer) {
    return this.circuitBreaker.executeSupplier(() -> consumer.apply(database));
  }

  public Jdbi getDatabase() {
    return database;
  }
}
