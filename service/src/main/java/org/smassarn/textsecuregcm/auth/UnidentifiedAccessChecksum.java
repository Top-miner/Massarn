/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.auth;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class UnidentifiedAccessChecksum {

  public static String generateFor(Optional<byte[]> unidentifiedAccessKey) {
    try {
      if (!unidentifiedAccessKey.isPresent()|| unidentifiedAccessKey.get().length != 16) return null;

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(unidentifiedAccessKey.get(), "HmacSHA256"));

      return Base64.getEncoder().encodeToString(mac.doFinal(new byte[32]));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }

}
