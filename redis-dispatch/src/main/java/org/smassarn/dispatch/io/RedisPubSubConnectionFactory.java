/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.dispatch.io;

import org.smassarn.dispatch.redis.PubSubConnection;

public interface RedisPubSubConnectionFactory {

  PubSubConnection connect();

}
