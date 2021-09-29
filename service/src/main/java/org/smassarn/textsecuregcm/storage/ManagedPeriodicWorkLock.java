/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.storage;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.smassarn.textsecuregcm.redis.ClusterLuaScript;
import org.smassarn.textsecuregcm.redis.FaultTolerantRedisCluster;

public class ManagedPeriodicWorkLock {

  private final String activeWorkerKey;

  private final FaultTolerantRedisCluster cacheCluster;
  private final ClusterLuaScript unlockClusterScript;

  public ManagedPeriodicWorkLock(final String activeWorkerKey, final FaultTolerantRedisCluster cacheCluster) throws IOException {
    this.activeWorkerKey = activeWorkerKey;
    this.cacheCluster = cacheCluster;
    this.unlockClusterScript = ClusterLuaScript.fromResource(cacheCluster, "lua/periodic_worker/unlock.lua", ScriptOutputType.INTEGER);
  }

  public boolean claimActiveWork(String workerId, Duration ttl) {
    return "OK".equals(cacheCluster.withCluster(connection -> connection.sync().set(activeWorkerKey, workerId, SetArgs.Builder.nx().px(ttl.toMillis()))));
  }

  public void releaseActiveWork(String workerId) {
    unlockClusterScript.execute(List.of(activeWorkerKey), List.of(workerId));
  }
}
