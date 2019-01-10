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
import static java.lang.String.format;

/**
 * A container for the messages dispatched to a certain consumer, such as an event subscriber
 * or a command handler.
 *
 * <p>Serves as a pre-stage allowing to filter, de-duplicate and reorder messages before they
 * are dispatched to their destination.
 */
public class Inbox {

    private final InboxId id;
    private final ImmutableMap<InboxLabel, DispatchOperation> operations;

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
        private final Map<InboxLabel, DispatchOperation> operations =
                new EnumMap<>(InboxLabel.class);

        /**
         * Creates an instance of {@code Builder} with the given consumer identifier.
         */
        private Builder(InboxId id) {
            inboxId = id;
        }

        public Builder add(InboxLabel label, DispatchOperation operation) {
            checkNotNull(label);
            checkNotNull(operation);
            operations.put(label, operation);
            return this;
        }

        public Inbox build() {
            return new Inbox(this);
        }
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

    private void storeOrDeliver(InboxLabel label, ActorMessageEnvelope<?, ?, ?> envelope) {
        ensureHasDestination(label);

        //TODO:2019-01-09:alex.tymchenko: store if windowing is enabled.
        // Deliver right away for now.

        operations.get(label)
                  .dispatch(envelope);
    }

    private void ensureHasDestination(InboxLabel label) {
        if (!operations.containsKey(label)) {
            throw new LabelNotFoundException(id, label);
        }
    }

    /**
     * Labels to put to the event in {@code Inbox}.
     *
     * <p>Determines how the event is going to be processed.
     */
    public class EventLabels {

        private final EventEnvelope envelope;

        private EventLabels(EventEnvelope envelope) {
            this.envelope = envelope;
        }

        /**
         * Marks the event envelope with a label for passing to an event reactor method.
         */
        public void toReact() {
            storeOrDeliver(InboxLabel.REACT_UPON_EVENT, envelope);
        }

        /**
         * Marks the event envelope with a label for passing to an event importer method.
         */
        public void toImport() {
            storeOrDeliver(InboxLabel.IMPORT_EVENT, envelope);
        }

        /**
         * Marks the event envelope with a label for passing to a method emitting commands in
         * response.
         */
        public void toCommand() {
            storeOrDeliver(InboxLabel.COMMAND_UPON_EVENT, envelope);
        }

        /**
         * Marks the event envelope with a label for passing to an event subscriber method.
         */
        public void forSubscriber() {
            storeOrDeliver(InboxLabel.UPDATE_SUBSCRIBER, envelope);
        }
    }

    /**
     * Labels to put to the command in {@code Inbox}.
     *
     * <p>Determines how the command is going to be processed.
     */
    public class CommandLabels {

        private final CommandEnvelope envelope;

        private CommandLabels(CommandEnvelope envelope) {
            this.envelope = envelope;
        }

        /**
         * Marks the command envelope with a label for passing to a command handler method.
         */
        public void toHandle() {
            storeOrDeliver(InboxLabel.HANDLE_COMMAND, envelope);
        }

        /**
         * Marks the command envelope with a label for passing to a method emitting commands in
         * response.
         */
        public void toTransform() {
            storeOrDeliver(InboxLabel.TRANSFORM_COMMAND, envelope);
        }
    }

    /**
     * Thrown if there is an attempt to mark a message put to {@code Inbox} with a label, which was
     * not {@linkplain io.spine.server.inbox.Inbox.Builder#add(InboxLabel,
     * io.spine.server.inbox.Inbox.DispatchOperation) added} for the {@code Inbox} instance.
     */
    public static class LabelNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final InboxLabel label;
        private final InboxId inboxId;

        public LabelNotFoundException(InboxId id, InboxLabel label) {
            this.label = label;
            inboxId = id;
        }

        @Override
        public String getMessage() {
            return format("Inbox %s has no available label %s",
                          Stringifiers.toString(inboxId), label);
        }
    }
}
