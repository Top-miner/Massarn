/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.push;

import static com.codahale.metrics.MetricRegistry.name;

import com.google.protobuf.ByteString;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.smassarn.textsecuregcm.storage.PubSubManager;
import org.smassarn.textsecuregcm.storage.PubSubProtos;
import org.smassarn.textsecuregcm.websocket.ProvisioningAddress;

public class ProvisioningManager {
    private final PubSubManager pubSubManager;

    private final Counter provisioningMessageOnlineCounter  = Metrics.counter(name(getClass(), "sendProvisioningMessage"), "online", "true");
    private final Counter provisioningMessageOfflineCounter = Metrics.counter(name(getClass(), "sendProvisioningMessage"), "online", "false");

    public ProvisioningManager(final PubSubManager pubSubManager) {
        this.pubSubManager = pubSubManager;
    }

    public boolean sendProvisioningMessage(ProvisioningAddress address, byte[] body) {
        PubSubProtos.PubSubMessage pubSubMessage = PubSubProtos.PubSubMessage.newBuilder()
                .setType(PubSubProtos.PubSubMessage.Type.DELIVER)
                .setContent(ByteString.copyFrom(body))
                .build();

        if (pubSubManager.publish(address, pubSubMessage)) {
            provisioningMessageOnlineCounter.increment();
            return true;
        } else {
            provisioningMessageOfflineCounter.increment();
            return false;
        }
    }
}
