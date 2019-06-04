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

import io.spine.server.sharding.ShardedMessageDelivery;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.inbox.InboxLabel.COMMAND_UPON_EVENT;
import static io.spine.server.inbox.InboxLabel.HANDLE_COMMAND;
import static io.spine.server.inbox.InboxLabel.IMPORT_EVENT;
import static io.spine.server.inbox.InboxLabel.REACT_UPON_EVENT;
import static io.spine.server.inbox.InboxLabel.TRANSFORM_COMMAND;
import static io.spine.server.inbox.InboxLabel.UPDATE_SUBSCRIBER;

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

    private final TypeUrl entityStateType;
    private final InboxOfCommands<I> commandPart;
    private final InboxOfEvents<I> eventPart;

    private Inbox(Builder<I> builder) {
        this.entityStateType = builder.entityStateType;
        this.commandPart = new InboxOfCommands<>(builder);
        this.eventPart = new InboxOfEvents<>(builder);
    }

    /**
     * Creates an instance of {@code Builder} with the given consumer identifier.
     *
     * @param typeUrl
     *         the type URL of a consumer
     */
    public static <I> Builder<I> newBuilder(TypeUrl typeUrl) {
        return new Builder<>(typeUrl);
    }

    /**
     * Sends an event envelope to the {@code Inbox} and allows to set a destination for this
     * message, determining its processing destiny.
     *
     * @param event
     *         the event to put to {@code Inbox}
     * @return the choice of destination for an event message available in this {@code Inbox}
     */
    public EventDestinations send(EventEnvelope event) {
        checkNotNull(event);
        return new EventDestinations(event);
    }

    /**
     * Sends a command envelope to the {@code Inbox} and allows to set a destination for this
     * message, determining its processing destiny.
     *
     * @param command
     *         the command to put to {@code Inbox}
     * @return the choice of destination for a command message available in this {@code Inbox}
     */
    public CommandDestinations send(CommandEnvelope command) {
        checkNotNull(command);
        return new CommandDestinations(command);
    }

    public TypeUrl getEntityStateType() {
        return entityStateType;
    }

    public ShardedMessageDelivery<InboxMessage> getProcessingBehavior() {
        return new InboxMessageDelivery();
    }

    /**
     * A builder of {@link Inbox} instances.
     *
     * @param <I>
     *         the type of identifier of the objects, for which the {@code Inbox} is built
     */
    public static class Builder<I> {

        private final TypeUrl entityStateType;
        private final Endpoints<I, EventEnvelope> eventEndpoints = new Endpoints<>();
        private final Endpoints<I, CommandEnvelope> commandEndpoints = new Endpoints<>();
        private InboxStorage storage;

        /**
         * Creates an instance of {@code Builder} for the given {@code Inbox} consumer entity type.
         *
         * @param type
         *         the type URL of the entity, to which belongs the {@code Inbox} being built
         */
        private Builder(TypeUrl type) {
            entityStateType = type;
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
            checkNotNull(entityStateType, "Entity state type must be set");
            checkNotNull(storage, "Inbox storage must be set");
            checkArgument(!eventEndpoints.isEmpty() || !commandEndpoints.isEmpty(),
                          "There must be at least one event or command endpoint");
            return new Inbox<>(this);
        }

        Endpoints<I, EventEnvelope> getEventEndpoints() {
            return eventEndpoints;
        }

        Endpoints<I, CommandEnvelope> getCommandEndpoints() {
            return commandEndpoints;
        }

        InboxStorage getStorage() {
            return storage;
        }

        TypeUrl getEntityStateType() {
            return entityStateType;
        }
    }

    /**
     * The available destinations for the {@code Event}s sent via this inbox.
     */
    public class EventDestinations {

        private final EventEnvelope event;

        private EventDestinations(EventEnvelope event) {
            this.event = event;
        }

        public void toReactor(I entityId) {
            eventPart.storeOrDeliver(event, entityId, REACT_UPON_EVENT);
        }

        public void toImporter(I entityId) {
            eventPart.storeOrDeliver(event, entityId, IMPORT_EVENT);
        }

        public void toCommander(I entityId) {
            eventPart.storeOrDeliver(event, entityId, COMMAND_UPON_EVENT);
        }

        public void toSubscriber(I entityId) {
            eventPart.storeOrDeliver(event, entityId, UPDATE_SUBSCRIBER);
        }
    }

    /**
     * The available destinations for the {@code Commands}s sent via this inbox.
     */
    public class CommandDestinations {

        private final CommandEnvelope command;

        private CommandDestinations(CommandEnvelope command) {
            this.command = command;
        }

        public void toHandler(I entityId) {
            commandPart.storeOrDeliver(command, entityId, HANDLE_COMMAND);
        }

        public void toCommander(I entityId) {
            commandPart.storeOrDeliver(command, entityId, TRANSFORM_COMMAND);
        }
    }

    /**
     * Takes the messages, which were previously sent to their targets via this inbox and
     * delivers them, performing their de-duplication.
     *
     * <p>Source messages for the de-duplication are supplied separately.
     */
    public class InboxMessageDelivery extends ShardedMessageDelivery<InboxMessage> {

        @Override
        protected void deliver(List<InboxMessage> incoming,
                               List<InboxMessage> deduplicationSource) {

            InboxPart.Dispatcher commandDispatcher =
                    commandPart.dispatcherWith(deduplicationSource);
            InboxPart.Dispatcher eventDispatcher =
                    eventPart.dispatcherWith(deduplicationSource);

            for (InboxMessage incomingMessage : incoming) {

                if(incomingMessage.hasCommand()) {
                    commandDispatcher.deliver(incomingMessage);
                } else {
                    eventDispatcher.deliver(incomingMessage);
                }
            }
        }
    }
}
