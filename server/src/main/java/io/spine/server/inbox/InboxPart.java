/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.inbox;

import io.spine.base.Time;
import io.spine.core.ActorMessageEnvelope;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.MessageEndpoint;

import java.util.Optional;

/**
 * An abstract base of {@link Inbox inbox} part.
 *
 * <p>Commands and events which are delivered to their targets through inbox are treated
 * in a similar way, but still there is some difference in storage mechanism and measures
 * taken in case of some runtime issues (e.g. duplication). Therefore the inbox is split into
 * parts, specific to each of the message types.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 * @param <M>
 *         the type of message envelopes, which are served by this inbox part
 */
abstract class InboxPart<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    private final M envelope;
    private final LabelledEndpoints<I, M> endpoints;
    private final InboxStorage storage;
    private final InboxId inboxId;
    private final I entityId;

    InboxPart(M envelope, Inbox.Builder<I> builder,
              LabelledEndpoints<I, M> endpoints, I entityId) {
        this.endpoints = endpoints;
        this.envelope = envelope;
        this.storage = builder.getStorage();
        this.inboxId = builder.getInboxId();
        this.entityId = entityId;
    }

    /**
     * Fetches the message object wrapped into the {@code envelope} and sets it as a payload of
     * the record further passed to storage.
     */
    protected abstract void setRecordPayload(M envelope, InboxMessageVBuilder builder);

    /**
     * Checks whether the message has already been stored in the inbox.
     *
     * <p>In case of duplication returns an {@code Optional} containing the duplication exception
     * wrapped into a inbox-specific runtime exception for further handling.
     */
    protected abstract Optional<CannotDeliverMessageException>
    checkDuplicates(InboxContentRecord contents);

    void storeOrDeliver(InboxLabel label) {
        MessageEndpoint<I, M> endpoint =
                endpoints.get(label, envelope)
                         .orElseThrow(() -> new LabelNotFoundException(inboxId, label));

        //TODO:2019-01-22:alex.tymchenko: read requests by label.
        Optional<InboxContentRecord> contents = storage.read(new InboxReadRequest(inboxId));
        if (contents.isPresent()) {
            InboxContentRecord contentRecord = contents.get();
            checkDuplicates(contentRecord).ifPresent(e -> endpoint.onError(envelope, e));
            return;
        }

        //TODO:2019-01-09:alex.tymchenko: store if windowing is enabled.
        // Deliver right away for now.
        if (ServerEnvironment.getInstance()
                             .getShardingStrategy()
                             .getShardCount() == 1) {
            endpoint.dispatchTo(entityId);
        }
        //TODO:2019-01-24:alex.tymchenko: set the sharding index!

        InboxMessageVBuilder builder = InboxMessageVBuilder
                .newBuilder()
                .setLabel(label)
                .setWhenReceived(Time.getCurrentTime());
        setRecordPayload(envelope, builder);
        InboxMessage message = builder
                .build();

        storage.write(inboxId, message);
    }

    protected M getEnvelope() {
        return envelope;
    }
}
