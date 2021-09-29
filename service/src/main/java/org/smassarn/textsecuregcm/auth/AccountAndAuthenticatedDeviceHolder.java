/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.auth;

import org.smassarn.textsecuregcm.storage.Account;
import org.smassarn.textsecuregcm.storage.Device;

public interface AccountAndAuthenticatedDeviceHolder {

  Account getAccount();

  Device getAuthenticatedDevice();
}
