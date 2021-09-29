/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.util;

public class TimestampHeaderUtil {

    public static final String TIMESTAMP_HEADER = "X-Massarn-Timestamp";

    private TimestampHeaderUtil() {
    }

    public static String getTimestampHeader() {
        return TIMESTAMP_HEADER + ":" + System.currentTimeMillis();
    }
}
