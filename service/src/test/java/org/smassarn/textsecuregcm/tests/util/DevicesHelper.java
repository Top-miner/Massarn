/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.tests.util;

import java.util.Random;
import org.smassarn.textsecuregcm.entities.SignedPreKey;
import org.smassarn.textsecuregcm.storage.Device;
import org.smassarn.textsecuregcm.util.Util;

public class DevicesHelper {

  private static final Random RANDOM = new Random();

  public static Device createDevice(final long deviceId) {
    final Device device = new Device(deviceId, null, null, null, null, null, null, false, 0, null, 0, 0, "OWT", 0,
        null);

    setEnabled(device, true);

    return device;
  }

  public static void setEnabled(Device device, boolean enabled) {
    if (enabled) {
      device.setSignedPreKey(new SignedPreKey(RANDOM.nextLong(), "testPublicKey-" + RANDOM.nextLong(),
          "testSignature-" + RANDOM.nextLong()));
      device.setGcmId("testGcmId" + RANDOM.nextLong());
      device.setLastSeen(Util.todayInMillis());
    } else {
      device.setSignedPreKey(null);
    }

    // fail fast, to guard against a change to the isEnabled() implementation causing unexpected test behavior
    assert enabled == device.isEnabled();
  }

}
