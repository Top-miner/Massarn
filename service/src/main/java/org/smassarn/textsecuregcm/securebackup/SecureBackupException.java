/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.securebackup;

public class SecureBackupException extends RuntimeException {

    public SecureBackupException(final String message) {
        super(message);
    }
}
