/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.util;

import io.lettuce.core.cluster.SlotHash;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RedisClusterUtilTest {

    @Test
    public void testGetMinimalHashTag() {
        for (int slot = 0; slot < SlotHash.SLOT_COUNT; slot++) {
            assertEquals(slot, SlotHash.getSlot(RedisClusterUtil.getMinimalHashTag(slot)));
        }
    }
}
