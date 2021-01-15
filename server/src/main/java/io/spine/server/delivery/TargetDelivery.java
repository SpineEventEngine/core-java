/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.delivery;

import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.core.TenantId;
import io.spine.server.tenant.IdInTenant;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.server.type.SignalEnvelope;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Takes the messages, which were previously sent to their targets via their inbox, and
 * delivers them.
 *
 * <p>Groups messages sent to the same target, but preserving the original order
 * throughout all the batches. Each of the resulting batches is delivered with a prior notification
 * of the supplied {@linkplain BatchDeliveryListener listener}. Underlying listener implementations
 * may then optimize loading of their targets, e.g. use a single read and single write operation
 * per batch.
 */
final class TargetDelivery<I> implements ShardedMessageDelivery<InboxMessage> {

    private final InboxOfCommands<I> inboxOfCmds;
    private final InboxOfEvents<I> inboxOfEvents;
    private final @Nullable BatchDeliveryListener<I> batchListener;

    TargetDelivery(InboxOfCommands<I> inboxOfCmds,
                   InboxOfEvents<I> inboxOfEvents,
                   @Nullable BatchDeliveryListener<I> batchListener) {
        this.inboxOfCmds = inboxOfCmds;
        this.inboxOfEvents = inboxOfEvents;
        this.batchListener = batchListener;
    }

    @Override
    public void deliver(List<InboxMessage> incoming) {

        if (batchListener == null) {
            for (InboxMessage incomingMessage : incoming) {
                doDeliver(inboxOfCmds, inboxOfEvents, incomingMessage);
            }
        } else {
            deliverInBatch(incoming, batchListener);
        }
    }

    @Override
    public void onDuplicate(InboxMessage message) {
        if(message.hasCommand()) {
            inboxOfCmds.notifyOfDuplicated(message);
        } else {
            inboxOfEvents.notifyOfDuplicated(message);
        }
    }

    private static <I> void doDeliver(InboxOfCommands<I>  cmdDispatcher,
                                      InboxOfEvents<I> eventDispatcher,
                                      InboxMessage incomingMessage) {
        if (incomingMessage.hasCommand()) {
            cmdDispatcher.deliver(incomingMessage);
        } else {
            eventDispatcher.deliver(incomingMessage);
        }
    }

    private void deliverInBatch(List<InboxMessage> incoming,
                                BatchDeliveryListener<I> batchDispatcher) {
        List<Batch<I>> batches = Batch.byInboxId(incoming, this::asEnvelope);

        for (Batch<I> batch : batches) {
            TenantId tenant = batch.inboxId.tenant();
            TenantAwareRunner.with(tenant).run(
                    () -> batch.deliverVia(batchDispatcher, inboxOfCmds, inboxOfEvents)
            );
        }
    }

    private SignalEnvelope<?, ?, ?> asEnvelope(InboxMessage message) {
        if (message.hasCommand()) {
            return inboxOfCmds.asEnvelope(message);
        } else {
            return inboxOfEvents.asEnvelope(message);
        }
    }

    /**
     * The batch of messages headed to the same target.
     */
    private static class Batch<I> {

        private final IdInTenant<InboxId> inboxId;
        private final List<InboxMessage> messages = new ArrayList<>();

        private Batch(InboxId inboxId, TenantId tenantId) {
            this.inboxId = IdInTenant.of(inboxId, tenantId);
        }

        /**
         * Groups the messages into batches by their {@code InboxId}s and {@code TenantId}.
         *
         * <p>The resulting order of messages through all batches is preserved.
         */
        private static <I> List<Batch<I>>
        byInboxId(List<InboxMessage> messages, Function<InboxMessage, SignalEnvelope<?, ?, ?>> fn) {
            List<Batch<I>> batches = new ArrayList<>();
            Batch<I> currentBatch = null;
            for (InboxMessage message : messages) {

                InboxId msgInboxId = message.getInboxId();
                SignalEnvelope<?, ?, ?> envelope = fn.apply(message);
                TenantId tenantId = envelope.tenantId();
                if (currentBatch == null) {
                    currentBatch = new Batch<>(msgInboxId, tenantId);
                } else {
                    if (!matchesBatch(currentBatch, msgInboxId, tenantId)) {
                        batches.add(currentBatch);
                        currentBatch = new Batch<>(msgInboxId, tenantId);
                    }
                }
                currentBatch.addMessage(message);
            }
            if (currentBatch != null) {
                batches.add(currentBatch);
            }
            return batches;
        }

        private static <I> boolean matchesBatch(Batch<I> currentBatch,
                                                InboxId msgInboxId,
                                                TenantId tenantId) {
            return (currentBatch.inboxId.value()
                                        .equals(msgInboxId)
                    && currentBatch.inboxId.tenant()
                                           .equals(tenantId));
        }

        private void addMessage(InboxMessage message) {
            messages.add(message);
        }

        private void deliverVia(BatchDeliveryListener<I> dispatcher,
                                InboxOfCommands<I> cmdDispatcher,
                                InboxOfEvents<I> eventDispatcher) {
            if (messages.size() > 1) {
                Any packedId = inboxId.value()
                                      .getEntityId()
                                      .getId();
                @SuppressWarnings("unchecked")      // Only IDs of type `I` are stored.
                        I id = (I) Identifier.unpack(packedId);
                dispatcher.onStart(id);
                try {
                    for (InboxMessage message : messages) {
                        doDeliver(cmdDispatcher, eventDispatcher, message);
                    }
                } finally {
                    dispatcher.onEnd(id);
                }
            } else {
                doDeliver(cmdDispatcher, eventDispatcher, messages.get(0));
            }
        }
    }
}
