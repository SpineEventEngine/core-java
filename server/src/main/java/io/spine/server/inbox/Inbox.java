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

import com.google.common.collect.ImmutableMap;
import io.spine.core.ActorMessageEnvelope;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.string.Stringifiers;

import java.util.EnumMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A container for the messages dispatched to a certain consumer, such as an event subscriber
 * or a command handler.
 *
 * <p>Serves as a pre-stage allowing to filter, de-duplicate and reorder messages before they
 * are dispatched to their destination.
 */
public class Inbox {

    private final InboxId id;
    private final ImmutableMap<MessageDestination, DispatchOperation> operations;

    private Inbox(Builder builder) {
        this.id = builder.inboxId;
        this.operations = ImmutableMap.copyOf(builder.operations);
    }

    /**
     * Creates an instance of {@code Builder} with the given consumer identifier.
     *
     * @param inboxId
     *         the identifier of the consumer for the messages in this {@code Inbox}.
     */
    public static Builder newBuilder(InboxId inboxId) {
        return new Builder(inboxId);
    }

    public interface DispatchOperation {

        void dispatch(ActorMessageEnvelope<?, ?, ?> envelope);
    }

    public static class Builder {

        private final InboxId inboxId;
        private final Map<MessageDestination, DispatchOperation> operations =
                new EnumMap<>(MessageDestination.class);

        /**
         * Creates an instance of {@code Builder} with the given consumer identifier.
         */
        private Builder(InboxId id) {
            inboxId = id;
        }

        public Builder add(MessageDestination destination, DispatchOperation operation) {
            checkNotNull(destination);
            checkNotNull(operation);
            operations.put(destination, operation);
            return this;
        }

        public Inbox build() {
            return new Inbox(this);
        }
    }

    public EventDestinations put(EventEnvelope envelope) {
        checkNotNull(envelope);
        return new EventDestinations(envelope);
    }

    public CommandDestinations put(CommandEnvelope envelope) {
        checkNotNull(envelope);
        return new CommandDestinations(envelope);
    }

    private void storeOrDeliver(MessageDestination destination,
                                ActorMessageEnvelope<?, ?, ?> envelope) {
        ensureHasDestination(destination);

        //TODO:2019-01-09:alex.tymchenko: store if windowing is enabled.
        // Deliver right away for now.

        operations.get(destination)
                  .dispatch(envelope);
    }

    private void ensureHasDestination(MessageDestination destination) {
        if (!operations.containsKey(destination)) {
            throw new DestinationNotAvailableException(id, destination);
        }
    }

    public class EventDestinations {

        private final EventEnvelope envelope;

        private EventDestinations(EventEnvelope envelope) {
            this.envelope = envelope;
        }

        public void toReact() {
            storeOrDeliver(MessageDestination.REACT_UPON_EVENT, envelope);
        }

        public void toImport() {
            storeOrDeliver(MessageDestination.IMPORT_EVENT, envelope);
        }

        public void toCommand() {
            storeOrDeliver(MessageDestination.COMMAND_UPON_EVENT, envelope);
        }

        public void forSubscriber() {
            storeOrDeliver(MessageDestination.UPDATE_SUBSCRIBER, envelope);
        }
    }

    public class CommandDestinations {

        private final CommandEnvelope envelope;

        private CommandDestinations(CommandEnvelope envelope) {
            this.envelope = envelope;
        }

        public void toHandle() {
            storeOrDeliver(MessageDestination.HANDLE_COMMAND, envelope);
        }

        public void toTransform() {
            storeOrDeliver(MessageDestination.TRANSFORM_COMMAND, envelope);
        }
    }

    public static class DestinationNotAvailableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final MessageDestination destination;
        private final InboxId inboxId;

        public DestinationNotAvailableException(InboxId id, MessageDestination destination) {
            this.destination = destination;
            inboxId = id;
        }

        @Override
        public String getMessage() {
            return String.format("Inbox %s has no available destination %s",
                                 Stringifiers.toString(inboxId), destination);
        }
    }
}
