/*
 * Copyright 2013-2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.smassarn.textsecuregcm.storage;

import static com.codahale.metrics.MetricRegistry.name;

import io.micrometer.core.instrument.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.ws.rs.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smassarn.textsecuregcm.entities.DirectoryReconciliationRequest;
import org.smassarn.textsecuregcm.entities.DirectoryReconciliationResponse;
import org.smassarn.textsecuregcm.entities.DirectoryReconciliationResponse.Status;

public class DirectoryReconciler extends AccountDatabaseCrawlerListener {

  private static final Logger logger = LoggerFactory.getLogger(DirectoryReconciler.class);
  private static final String SEND_TIMER_NAME = name(DirectoryReconciler.class, "sendRequest");

  private final String replicationName;
  private final DirectoryReconciliationClient reconciliationClient;

  public DirectoryReconciler(String replicationName, DirectoryReconciliationClient reconciliationClient) {
    this.reconciliationClient = reconciliationClient;
    this.replicationName = replicationName;
  }

  @Override
  public void onCrawlStart() {
  }

  @Override
  public void onCrawlEnd(Optional<UUID> fromUuid) {
    reconciliationClient.complete();
  }

  @Override
  protected void onCrawlChunk(final Optional<UUID> fromUuid, final List<Account> accounts)
      throws AccountDatabaseCrawlerRestartException {

    final DirectoryReconciliationRequest addUsersRequest;
    final DirectoryReconciliationRequest deleteUsersRequest;
    {
      final List<DirectoryReconciliationRequest.User> addedUsers = new ArrayList<>(accounts.size());
      final List<DirectoryReconciliationRequest.User> deletedUsers = new ArrayList<>(accounts.size());

      accounts.forEach(account -> {
        if (account.shouldBeVisibleInDirectory()) {
          addedUsers.add(new DirectoryReconciliationRequest.User(account.getUuid(), account.getNumber()));
        } else {
          deletedUsers.add(new DirectoryReconciliationRequest.User(account.getUuid(), account.getNumber()));
        }
      });

      addUsersRequest = new DirectoryReconciliationRequest(addedUsers);
      deleteUsersRequest = new DirectoryReconciliationRequest(deletedUsers);
    }

    final DirectoryReconciliationResponse addUsersResponse = sendAdditions(addUsersRequest);
    final DirectoryReconciliationResponse deleteUsersResponse = sendDeletes(deleteUsersRequest);

    if (addUsersResponse.getStatus() == DirectoryReconciliationResponse.Status.MISSING
        || deleteUsersResponse.getStatus() == Status.MISSING) {

      throw new AccountDatabaseCrawlerRestartException("directory reconciler missing");
    }
  }

  private DirectoryReconciliationResponse sendDeletes(final DirectoryReconciliationRequest request) {
    return sendRequest(request, reconciliationClient::delete, "delete");

  }

  private DirectoryReconciliationResponse sendAdditions(final DirectoryReconciliationRequest request) {
    return sendRequest(request, reconciliationClient::add, "add");
  }

  private DirectoryReconciliationResponse sendRequest(final DirectoryReconciliationRequest request,
      final Function<DirectoryReconciliationRequest, DirectoryReconciliationResponse> requestHandler,
      final String context) {

    return Metrics.timer(SEND_TIMER_NAME, "context", context, "replication", replicationName)
        .record(() -> {
          try {
            final DirectoryReconciliationResponse response = requestHandler.apply(request);

            if (response.getStatus() != DirectoryReconciliationResponse.Status.OK) {
              logger.warn("reconciliation error: {} ({})", response.getStatus(), context);
            }
            return response;
          } catch (ProcessingException ex) {
            logger.warn("request error: ", ex);
            throw new ProcessingException(ex);
          }
        });
  }

}
