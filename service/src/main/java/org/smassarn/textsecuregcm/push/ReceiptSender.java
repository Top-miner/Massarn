/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.push;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smassarn.textsecuregcm.auth.AuthenticatedAccount;
import org.smassarn.textsecuregcm.controllers.NoSuchUserException;
import org.smassarn.textsecuregcm.entities.MessageProtos.Envelope;
import org.smassarn.textsecuregcm.storage.Account;
import org.smassarn.textsecuregcm.storage.AccountsManager;
import org.smassarn.textsecuregcm.storage.Device;

public class ReceiptSender {

  private final MessageSender   messageSender;
  private final AccountsManager accountManager;

  private static final Logger logger = LoggerFactory.getLogger(ReceiptSender.class);

  public ReceiptSender(final AccountsManager accountManager, final MessageSender messageSender) {
    this.accountManager = accountManager;
    this.messageSender = messageSender;
  }

  public void sendReceipt(AuthenticatedAccount source, UUID destinationUuid, long messageId) throws NoSuchUserException {
    final Account sourceAccount = source.getAccount();
    if (sourceAccount.getUuid().equals(destinationUuid)) {
      return;
    }

    final Account destinationAccount = accountManager.get(destinationUuid)
        .orElseThrow(() -> new NoSuchUserException(destinationUuid));

    final Envelope.Builder message = Envelope.newBuilder()
        .setServerTimestamp(System.currentTimeMillis())
        .setSource(sourceAccount.getNumber())
        .setSourceUuid(sourceAccount.getUuid().toString())
        .setSourceDevice((int) source.getAuthenticatedDevice().getId())
        .setTimestamp(messageId)
        .setType(Envelope.Type.SERVER_DELIVERY_RECEIPT);

    if (sourceAccount.getRelay().isPresent()) {
      message.setRelay(sourceAccount.getRelay().get());
    }

    for (final Device destinationDevice : destinationAccount.getDevices()) {
      try {
        messageSender.sendMessage(destinationAccount, destinationDevice, message.build(), false);
      } catch (final NotPushRegisteredException e) {
        logger.info("User no longer push registered for delivery receipt: " + e.getMessage());
      }
    }
  }
}
