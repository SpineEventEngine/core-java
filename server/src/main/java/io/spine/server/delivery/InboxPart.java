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

package io.spine.server.delivery;

import io.spine.base.Time;
import io.spine.server.ServerEnvironment;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.server.type.ActorMessageEnvelope;
import io.spine.string.Stringifiers;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An abstract base of {@link Inbox inbox} part.
 *
 * <p>Commands and events which are delivered to their targets through inbox are treated
 * in a similar way, but still there is some difference in storage mechanism and measures
 * taken in case of some runtime issues (e.g. duplication). Therefore the inbox is split into
 * parts, specific to each of the message types.
 *
 * <p>Descendants define the behaviour specific to the signal type.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 * @param <M>
 *         the type of message envelopes, which are served by this inbox part
 */
abstract class InboxPart<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    private final Endpoints<I, M> endpoints;
    private final InboxWriter writer;
    private final TypeUrl entityStateType;

    InboxPart(Inbox.Builder<I> builder, Endpoints<I, M> endpoints) {
        this.endpoints = endpoints;
        this.writer = builder.writer();
        this.entityStateType = builder.getEntityStateType();
    }

    /**
     * Fetches the message object wrapped into the {@code envelope} and sets it as a payload of
     * the record further passed to storage.
     */
    protected abstract void setRecordPayload(M envelope, InboxMessage.Builder builder);

    /**
     * Extracts the UUID identifier value from the envelope wrapping the passed object.
     */
    protected abstract String extractUuidFrom(M envelope);

    /**
     * Extracts the message object passed inside the {@code InboxMessage} as an envelope.
     */
    protected abstract M asEnvelope(InboxMessage message);

    /**
     * Determines the status of the message.
     */
    protected InboxMessageStatus getStatus(M message) {
        return InboxMessageStatus.TO_DELIVER;
    }

    /**
     * Creates an instance of dispatcher along with the de-duplication source messages.
     */
    protected abstract Dispatcher dispatcherWith(Collection<InboxMessage> deduplicationSource);

    void store(M envelope, I entityId, InboxLabel label) {
        InboxId inboxId = InboxIds.wrap(entityId, entityStateType);
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        ShardIndex shardIndex = delivery.whichShardFor(entityId);
        InboxMessage.Builder builder = InboxMessage
                .newBuilder()
                .setId(InboxMessageId.generate())
                .setSignalId(signalIdFrom(envelope, entityId))
                .setInboxId(inboxId)
                .setShardIndex(shardIndex)
                .setLabel(label)
                .setStatus(getStatus(envelope))
                .setWhenReceived(Time.currentTime());
        setRecordPayload(envelope, builder);
        InboxMessage message = builder.vBuild();

        TenantAwareRunner
                .with(envelope.tenantId())
                .run(() -> {
                    writer.write(message);
                });
    }

    private InboxSignalId signalIdFrom(M envelope, I targetId) {
        String rawValue = extractUuidFrom(envelope) + " @" + Stringifiers.toString(targetId);
        InboxSignalId result = InboxSignalId.newBuilder()
                                            .setValue(rawValue)
                                            .build();
        return result;
    }

    /**
     * An abstract base for routines which dispatch {@code InboxMessage}s to their endpoints.
     *
     *
     * <p>Takes care of de-duplication of the messages, using the prepared collection of previously
     * dispatched messages to look for duplicate amongst.
     *
     * <p>In case a duplication is found, the respective endpoint is
     * {@linkplain MessageEndpoint#onError(ActorMessageEnvelope, RuntimeException) notified}.
     */
    abstract class Dispatcher {

        /**
         * A set of IDs of previously dispatched messages, stored as {@code String} values.
         */
        private final Set<String> rawIds;

        @SuppressWarnings({ /* To avoid extra filtering in descendants and improve performance. */
                "AbstractMethodCallInConstructor",
                "OverridableMethodCallDuringObjectConstruction",
                "OverriddenMethodCallDuringObjectConstruction"})
        Dispatcher(Collection<InboxMessage> deduplicationSource) {
            this.rawIds =
                    deduplicationSource
                            .stream()
                            .filter(filterByType())
                            .map(InboxMessage::getSignalId)
                            .map(InboxSignalId::getValue)
                            .collect(Collectors.toCollection((Supplier<Set<String>>) HashSet::new));
        }

        protected abstract Predicate<? super InboxMessage> filterByType();

        void deliver(InboxMessage message) {
            Optional<? extends RuntimeException> duplicationException = checkDuplicate(message);
            M envelope = asEnvelope(message);
            InboxLabel label = message.getLabel();
            InboxId inboxId = message.getInboxId();
            MessageEndpoint<I, M> endpoint =
                    endpoints.get(label, envelope)
                             .orElseThrow(() -> new LabelNotFoundException(inboxId, label));

            TenantAwareRunner
                    .with(envelope.tenantId())
                    .run(() -> {
                        if (duplicationException.isPresent()) {
                            endpoint.onError(envelope, duplicationException.get());
                        } else {
                            @SuppressWarnings("unchecked")    // Only IDs of type `I` are stored.
                                    I unpackedId = (I) InboxIds.unwrap(message.getInboxId());
                            endpoint.dispatchTo(unpackedId);
                        }
                    });
        }

        /**
         * Emits a exception, specific to the type of the message, in case the passed message
         * is determined to be a duplicate.
         *
         * @param duplicate
         *         the duplicate message
         * @return a type-specific exception
         */
        protected abstract RuntimeException onDuplicateFound(InboxMessage duplicate);

        /**
         * Checks whether the message has already been stored in the inbox.
         *
         * <p>In case of duplication returns an {@code Optional} containing the duplication
         * exception wrapped into a inbox-specific runtime exception for further handling.
         */
        private Optional<? extends RuntimeException> checkDuplicate(InboxMessage message) {
            String currentId = message.getSignalId()
                                      .getValue();
            boolean hasDuplicate = rawIds.contains(currentId);
            if (hasDuplicate) {
                RuntimeException exception = onDuplicateFound(message);
                return Optional.of(exception);
            }
            return Optional.empty();
        }
    }
}
