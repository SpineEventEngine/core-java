/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A container for the messages dispatched to a certain consumer, such as an event subscriber
 * or a command handler.
 *
 * <p>Serves as a pre-stage allowing to filter, de-duplicate and reorder messages before they
 * are dispatched to their destination.
 *
 * @param <I>
 *         the type of consumer identifiers.
 */
public class Inbox<I> {

    private final I entityId;
    private final Builder<I> builder;

    private Inbox(Builder<I> builder) {
        this.builder = builder;
        this.entityId = toEntityId(builder.getInboxId());
    }

    @SuppressWarnings("unchecked")  // Ensured by the `Inbox` definition.
    private I toEntityId(InboxId inboxId) {
        return (I) InboxIds.unwrap(inboxId);
    }

    /**
     * Creates an instance of {@code Builder} with the given consumer identifier.
     *
     * @param id
     *         the identifier of a consumer
     * @param typeUrl
     *         the type URL of a consumer
     */
    public static <I> Builder<I> newBuilder(Object id, TypeUrl typeUrl) {
        InboxId inboxId = InboxIds.wrap(id, typeUrl);
        return new Builder<>(inboxId);
    }

    /**
     * Puts an event envelope to the {@code Inbox} and allows to set a label for this message,
     * determining its processing destiny.
     *
     * @param envelope
     *         the event to put to {@code Inbox}
     * @return the choice of labels for an event message available in this {@code Inbox}
     */
    public EventLabels put(EventEnvelope envelope) {
        checkNotNull(envelope);
        return new EventLabels(envelope);
    }

    /**
     * Puts a command envelope to the {@code Inbox} and allows to set a label for this message,
     * determining its processing destiny.
     *
     * @param envelope
     *         the event to put to {@code Inbox}
     * @return the choice of labels for a command message available in this {@code Inbox}
     */
    public CommandLabels put(CommandEnvelope envelope) {
        checkNotNull(envelope);
        return new CommandLabels(envelope);
    }

    /**
     * A builder of {@link Inbox} instances.
     *
     * @param <I>
     *         the type of identifier of the objects, for which the {@code Inbox} is built
     */
    public static class Builder<I> {

        private final InboxId inboxId;
        private final LabelledEndpoints<I, EventEnvelope> eventEndpoints = new LabelledEndpoints<>();
        private final LabelledEndpoints<I, CommandEnvelope> commandEndpoints = new LabelledEndpoints<>();
        private InboxStorage storage;

        /**
         * Creates an instance of {@code Builder} with the given consumer identifier.
         */
        private Builder(InboxId id) {
            inboxId = id;
        }

        /**
         * Adds a certain label for the {@code Inbox} and specify a lazy-initialized endpoint,
         * to which the respectively labelled events should be delivered.
         */
        public Builder<I> addEventEndpoint(InboxLabel label,
                                           LazyEndpoint<I, EventEnvelope> lazyEndpoint) {
            checkNotNull(label);
            checkNotNull(lazyEndpoint);
            eventEndpoints.add(label, lazyEndpoint);
            return this;
        }

        /**
         * Adds a certain label for the {@code Inbox} and specify a lazy-initialized endpoint,
         * to which the respectively labelled commands should be delivered.
         */
        public Builder<I> addCommandEndpoint(InboxLabel label,
                                             LazyEndpoint<I, CommandEnvelope> lazyEndpoint) {
            checkNotNull(label);
            checkNotNull(lazyEndpoint);
            commandEndpoints.add(label, lazyEndpoint);
            return this;
        }

        public Builder<I> setStorage(InboxStorage storage) {
            this.storage = checkNotNull(storage);
            return this;
        }

        public Inbox<I> build() {
            return new Inbox<>(this);
        }

        InboxId getInboxId() {
            return inboxId;
        }

        LabelledEndpoints<I, EventEnvelope> getEventEndpoints() {
            return eventEndpoints;
        }

        LabelledEndpoints<I, CommandEnvelope> getCommandEndpoints() {
            return commandEndpoints;
        }

        InboxStorage getStorage() {
            return storage;
        }
    }

    /**
     * Labels to put to the event in {@code Inbox}.
     *
     * <p>Determines how the event is going to be processed.
     */
    public class EventLabels {

        private final InboxOfEvents handler;

        private EventLabels(EventEnvelope envelope) {
            this.handler = new InboxOfEvents<>(envelope, builder, entityId);
        }

        /**
         * Marks the event envelope with a label for passing to an event reactor method.
         */
        public void toReact() {
            handler.storeOrDeliver(InboxLabel.REACT_UPON_EVENT);
        }

        /**
         * Marks the event envelope with a label for passing to an event importer method.
         */
        public void toImport() {
            handler.storeOrDeliver(InboxLabel.IMPORT_EVENT);
        }

        /**
         * Marks the event envelope with a label for passing to a method emitting commands in
         * response.
         */
        public void toCommand() {
            handler.storeOrDeliver(InboxLabel.COMMAND_UPON_EVENT);
        }

        /**
         * Marks the event envelope with a label for passing to an event subscriber method.
         */
        public void forSubscriber() {
            handler.storeOrDeliver(InboxLabel.UPDATE_SUBSCRIBER);
        }
    }

    /**
     * Labels to put to the command in {@code Inbox}.
     *
     * <p>Determines how the command is going to be processed.
     */
    public class CommandLabels {

        private final InboxOfCommands handler;

        private CommandLabels(CommandEnvelope envelope) {
            this.handler = new InboxOfCommands<>(envelope, builder, entityId);
        }

        /**
         * Marks the command envelope with a label for passing to a command handler method.
         */
        public void toHandle() {
            handler.storeOrDeliver(InboxLabel.HANDLE_COMMAND);
        }

        /**
         * Marks the command envelope with a label for passing to a method emitting commands in
         * response.
         */
        public void toTransform() {
            handler.storeOrDeliver(InboxLabel.TRANSFORM_COMMAND);
        }
    }
}
