/*
 * Copyright 2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.securestorage;

public class SecureStorageException extends RuntimeException {

    public SecureStorageException(final String message) {
        super(message);
    }
}
