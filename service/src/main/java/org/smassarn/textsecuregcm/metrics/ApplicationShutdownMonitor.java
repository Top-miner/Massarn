/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import io.dropwizard.lifecycle.Managed;
import io.micrometer.core.instrument.Metrics;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A managed monitor that reports whether the application is shutting down as a metric. That metric can then be used in
 * conjunction with other indicators to conditionally fire or suppress alerts.
 */
public class ApplicationShutdownMonitor implements Managed {

  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

  public ApplicationShutdownMonitor() {
    Metrics.gauge(name(getClass().getSimpleName(), "shuttingDown"), shuttingDown, b -> b.get() ? 1 : 0);
  }

  @Override
  public void start() throws Exception {
    shuttingDown.set(false);
  }

  @Override
  public void stop() throws Exception {
    shuttingDown.set(true);
  }
}
