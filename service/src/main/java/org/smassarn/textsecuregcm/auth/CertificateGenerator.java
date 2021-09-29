/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.auth;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.smassarn.textsecuregcm.crypto.Curve;
import org.smassarn.textsecuregcm.crypto.ECPrivateKey;
import org.smassarn.textsecuregcm.entities.MessageProtos.SenderCertificate;
import org.smassarn.textsecuregcm.entities.MessageProtos.ServerCertificate;
import org.smassarn.textsecuregcm.storage.Account;
import org.smassarn.textsecuregcm.storage.Device;

public class CertificateGenerator {

  private final ECPrivateKey      privateKey;
  private final int               expiresDays;
  private final ServerCertificate serverCertificate;

  public CertificateGenerator(byte[] serverCertificate, ECPrivateKey privateKey, int expiresDays)
      throws InvalidProtocolBufferException
  {
    this.privateKey        = privateKey;
    this.expiresDays       = expiresDays;
    this.serverCertificate = ServerCertificate.parseFrom(serverCertificate);
  }

  public byte[] createFor(Account account, Device device, boolean includeE164) throws InvalidKeyException {
    SenderCertificate.Certificate.Builder builder = SenderCertificate.Certificate.newBuilder()
                                                                                 .setSenderDevice(Math.toIntExact(device.getId()))
                                                                                 .setExpires(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expiresDays))
                                                                                 .setIdentityKey(ByteString.copyFrom(Base64.getDecoder().decode(account.getIdentityKey())))
                                                                                 .setSigner(serverCertificate)
                                                                                 .setSenderUuid(account.getUuid().toString());

    if (includeE164) {
      builder.setSender(account.getNumber());
    }

    byte[] certificate = builder.build().toByteArray();
    byte[] signature   = Curve.calculateSignature(privateKey, certificate);

    return SenderCertificate.newBuilder()
                            .setCertificate(ByteString.copyFrom(certificate))
                            .setSignature(ByteString.copyFrom(signature))
                            .build()
                            .toByteArray();
  }

}
