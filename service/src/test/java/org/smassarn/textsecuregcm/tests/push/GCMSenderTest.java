/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.tests.push;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.smassarn.gcm.server.Message;
import org.smassarn.gcm.server.Result;
import org.smassarn.gcm.server.Sender;
import org.smassarn.textsecuregcm.push.GCMSender;
import org.smassarn.textsecuregcm.push.GcmMessage;
import org.smassarn.textsecuregcm.storage.Account;
import org.smassarn.textsecuregcm.storage.AccountsManager;
import org.smassarn.textsecuregcm.storage.Device;
import org.smassarn.textsecuregcm.tests.util.AccountsHelper;
import org.smassarn.textsecuregcm.tests.util.SynchronousExecutorService;
import org.smassarn.textsecuregcm.util.Util;

public class GCMSenderTest {

  @Test
  public void testSendMessage() {
    AccountsManager            accountsManager = mock(AccountsManager.class);
    Sender                     sender          = mock(Sender.class         );
    Result                     successResult   = mock(Result.class         );
    SynchronousExecutorService executorService = new SynchronousExecutorService();

    when(successResult.isInvalidRegistrationId()).thenReturn(false);
    when(successResult.isUnregistered()).thenReturn(false);
    when(successResult.hasCanonicalRegistrationId()).thenReturn(false);
    when(successResult.isSuccess()).thenReturn(true);

    AccountsHelper.setupMockUpdate(accountsManager);

    GcmMessage message = new GcmMessage("foo", UUID.randomUUID(), 1, GcmMessage.Type.NOTIFICATION, Optional.empty());
    GCMSender gcmSender = new GCMSender(executorService, accountsManager, sender);

    CompletableFuture<Result> successFuture = CompletableFuture.completedFuture(successResult);

    when(sender.send(any(Message.class))).thenReturn(successFuture);

    gcmSender.sendMessage(message);

    verify(sender, times(1)).send(any(Message.class));
  }

  @Test
  public void testSendUninstalled() {
    UUID destinationUuid = UUID.randomUUID();
    String gcmId = "foo";

    AccountsManager            accountsManager = mock(AccountsManager.class);
    Sender                     sender          = mock(Sender.class         );
    Result                     invalidResult   = mock(Result.class         );
    SynchronousExecutorService executorService = new SynchronousExecutorService();

    Account destinationAccount = mock(Account.class);
    Device  destinationDevice  = mock(Device.class );

    AccountsHelper.setupMockUpdate(accountsManager);

    when(destinationAccount.getDevice(1)).thenReturn(Optional.of(destinationDevice));
    when(accountsManager.get(destinationUuid)).thenReturn(Optional.of(destinationAccount));
    when(destinationDevice.getGcmId()).thenReturn(gcmId);

    when(invalidResult.isInvalidRegistrationId()).thenReturn(true);
    when(invalidResult.isUnregistered()).thenReturn(false);
    when(invalidResult.hasCanonicalRegistrationId()).thenReturn(false);
    when(invalidResult.isSuccess()).thenReturn(true);

    GcmMessage message = new GcmMessage(gcmId, destinationUuid, 1, GcmMessage.Type.NOTIFICATION, Optional.empty());
    GCMSender gcmSender = new GCMSender(executorService, accountsManager, sender);

    CompletableFuture<Result> invalidFuture = CompletableFuture.completedFuture(invalidResult);

    when(sender.send(any(Message.class))).thenReturn(invalidFuture);

    gcmSender.sendMessage(message);

    verify(sender, times(1)).send(any(Message.class));
    verify(accountsManager, times(1)).get(eq(destinationUuid));
    verify(accountsManager, times(1)).updateDevice(eq(destinationAccount), eq(1L), any());
    verify(destinationDevice, times(1)).setUninstalledFeedbackTimestamp(eq(Util.todayInMillis()));
  }

  @Test
  public void testCanonicalId() {
    UUID destinationUuid     = UUID.randomUUID();
    String gcmId             = "foo";
    String canonicalId       = "bar";

    AccountsManager            accountsManager = mock(AccountsManager.class);
    Sender                     sender          = mock(Sender.class         );
    Result                     canonicalResult = mock(Result.class         );
    SynchronousExecutorService executorService = new SynchronousExecutorService();

    Account        destinationAccount = mock(Account.class       );
    Device         destinationDevice  = mock(Device.class        );

    when(destinationAccount.getDevice(1)).thenReturn(Optional.of(destinationDevice));
    when(accountsManager.get(destinationUuid)).thenReturn(Optional.of(destinationAccount));
    when(destinationDevice.getGcmId()).thenReturn(gcmId);

    AccountsHelper.setupMockUpdate(accountsManager);

    when(canonicalResult.isInvalidRegistrationId()).thenReturn(false);
    when(canonicalResult.isUnregistered()).thenReturn(false);
    when(canonicalResult.hasCanonicalRegistrationId()).thenReturn(true);
    when(canonicalResult.isSuccess()).thenReturn(false);
    when(canonicalResult.getCanonicalRegistrationId()).thenReturn(canonicalId);

    GcmMessage message = new GcmMessage(gcmId, destinationUuid, 1, GcmMessage.Type.NOTIFICATION, Optional.empty());
    GCMSender gcmSender = new GCMSender(executorService, accountsManager, sender);

    CompletableFuture<Result> invalidFuture = CompletableFuture.completedFuture(canonicalResult);

    when(sender.send(any(Message.class))).thenReturn(invalidFuture);

    gcmSender.sendMessage(message);

    verify(sender, times(1)).send(any(Message.class));
    verify(accountsManager, times(1)).get(eq(destinationUuid));
    verify(accountsManager, times(1)).updateDevice(eq(destinationAccount), eq(1L), any());
    verify(destinationDevice, times(1)).setGcmId(eq(canonicalId));
  }

}
