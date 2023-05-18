/*
 * Copyright 2023, TeamDev. All rights reserved.
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
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.TenantId;
import io.spine.server.delivery.FailedReception.Action;
import io.spine.server.dispatch.DispatchOutcome;
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
    public void deliver(List<InboxMessage> incoming, DeliveryMonitor monitor, Conveyor conveyor) {
        MonitoringDispatcher<I> dispatcher =
                new MonitoringDispatcher<>(monitor, conveyor, inboxOfCmds, inboxOfEvents);
        if (batchListener == null) {
            for (InboxMessage incomingMessage : incoming) {
                dispatcher.dispatch(incomingMessage);
            }
        } else {
            deliverInBatch(incoming, dispatcher, batchListener);
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

    private void deliverInBatch(List<InboxMessage> incoming,
                                MonitoringDispatcher<I> dispatcher,
                                BatchDeliveryListener<I> batchDispatcher) {
        List<Batch<I>> batches = Batch.byInboxId(incoming, dispatcher, this::asEnvelope);

        for (Batch<I> batch : batches) {
            TenantId tenant = batch.inboxId.tenant();
            TenantAwareRunner.with(tenant).run(
                () -> batch.deliverWith(batchDispatcher)
            );
        }
    }

    @SuppressWarnings("rawtypes")   /* For simplicity. */
    private SignalEnvelope asEnvelope(InboxMessage message) {
        if (message.hasCommand()) {
            return inboxOfCmds.asEnvelope(message);
        } else {
            return inboxOfEvents.asEnvelope(message);
        }
    }

    /**
     * Dispatches the signal to the respective target, monitors
     * the erroneous {@code DispatchOutcome}s and notifies the {@code DeliveryMonitor} of such.
     *
     * @param <I>
     *         type of identifiers of the delivery targets
     */
    private static class MonitoringDispatcher<I> {

        private final DeliveryMonitor monitor;
        private final Conveyor conveyor;
        private final InboxOfCommands<I> inboxOfCommands;
        private final InboxOfEvents<I> inboxOfEvents;


        private MonitoringDispatcher(DeliveryMonitor monitor,
                                     Conveyor conveyor,
                                     InboxOfCommands<I> inboxOfCommands,
                                     InboxOfEvents<I> inboxOfEvents) {
            this.monitor = monitor;
            this.conveyor = conveyor;
            this.inboxOfCommands = inboxOfCommands;
            this.inboxOfEvents = inboxOfEvents;
        }

        private void dispatch(InboxMessage message) {
            DispatchOutcome outcome = doDispatch(message);
            if(outcome.hasError()) {
                Error error = outcome.getError();
                FailedReception reception =
                        new FailedReception(message, error, conveyor, () -> dispatch(message));
                Action action = monitor.onReceptionFailure(reception);
                action.execute();
            }
        }

        private DispatchOutcome doDispatch(InboxMessage message) {
            return message.hasCommand()
                   ? inboxOfCommands.deliver(message)
                   : inboxOfEvents.deliver(message);
        }
    }

    /**
     * The batch of messages headed to the same target.
     */
    private static class Batch<I> {

        private final IdInTenant<InboxId> inboxId;
        private final List<InboxMessage> messages = new ArrayList<>();
        private final MonitoringDispatcher<I> dispatcher;


        private Batch(InboxId inboxId, TenantId tenantId, MonitoringDispatcher<I> dispatcher) {
            this.inboxId = IdInTenant.of(inboxId, tenantId);
            this.dispatcher = dispatcher;
        }

        /**
         * Groups the messages into batches by their {@code InboxId}s and {@code TenantId}.
         *
         * <p>The resulting order of messages through all batches is preserved.
         */
        @SuppressWarnings("rawtypes")   /* For simplicity. */
        private static <I> List<Batch<I>>
        byInboxId(List<InboxMessage> messages,
                  MonitoringDispatcher<I> dispatcher,
                  Function<InboxMessage, SignalEnvelope> toEnvelope) {
            List<Batch<I>> batches = new ArrayList<>();
            Batch<I> currentBatch = null;
            for (InboxMessage message : messages) {

                InboxId msgInboxId = message.getInboxId();
                SignalEnvelope envelope = toEnvelope.apply(message);
                TenantId tenantId = envelope.tenantId();
                if (currentBatch == null) {
                    currentBatch = new Batch<>(msgInboxId, tenantId, dispatcher);
                } else {
                    if (!matchesBatch(currentBatch, msgInboxId, tenantId)) {
                        batches.add(currentBatch);
                        currentBatch = new Batch<>(msgInboxId, tenantId, dispatcher);
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

        private void deliverWith(BatchDeliveryListener<I> listener) {
            if (messages.size() > 1) {
                Any packedId = inboxId.value()
                                      .getEntityId()
                                      .getId();
                @SuppressWarnings("unchecked")      // Only IDs of type `I` are stored.
                I id = (I) Identifier.unpack(packedId);
                listener.onStart(id);
                try {
                    for (InboxMessage message : messages) {
                        dispatcher.dispatch(message);
                    }
                } finally {
                    listener.onEnd(id);
                }
            } else {
                dispatcher.dispatch(messages.get(0));
            }
        }
    }
}
