/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Version;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Wrapper;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.integration.IntegrationEventContext;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.time.Time.getCurrentTime;
import static org.spine3.validate.Validate.checkValid;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * Produces events in response to a command.
 *
 * @author Alexander Yevsyukov
 */
public class EventFactory {

    private final Any producerId;
    private final CommandContext commandContext;
    private final EventIdSequence idSequence;

    protected EventFactory(Builder builder) {
        this.producerId = builder.producerId;
        this.commandContext = builder.commandContext;
        this.idSequence = EventIdSequence.on(builder.commandId)
                                         .withMaxSize(builder.maxEventCount);
    }

    /**
     * Creates an event for the passed event message.
     *
     * <p>The message passed is validated according to the constraints set in its Protobuf
     * definition. In case the message isn't valid, an {@linkplain ConstraintViolationThrowable
     * exception} is thrown.
     *
     * <p>In the message is an instance of {@code Any}, it is unpacked for validation.
     *
     * <p>It is recommended to use a corresponding {@linkplain org.spine3.validate.ValidatingBuilder
     * validating Builder} implementation to create the message.
     *
     * @param messageOrAny the message of the event or the message packed into {@code Any}
     * @param version      the version of the entity which produces the event
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    public Event createEvent(Message messageOrAny,
                             @Nullable Version version) throws ConstraintViolationThrowable {
        checkNotNull(messageOrAny);
        validate(messageOrAny);     // we must validate it now before emitting the next ID.

        final EventId eventId = idSequence.next();
        final EventContext context = createContext(producerId, commandContext, version);
        final Event result = createEvent(eventId, messageOrAny, context);
        return result;
    }

    private static void validate(Message messageOrAny) throws ConstraintViolationThrowable {
        final Message toValidate;
        toValidate = messageOrAny instanceof Any
                ? AnyPacker.unpack((Any) messageOrAny)
                : messageOrAny;
        checkValid(toValidate);
    }

    /**
     * Creates a new {@code Event} instance.
     *
     * @param id           the ID of the event
     * @param messageOrAny the event message or {@code Any} containing the message
     * @param context      the event context
     * @return created event instance
     */
    private static Event createEvent(EventId id, Message messageOrAny, EventContext context) {
        checkNotNull(messageOrAny);
        checkNotNull(context);
        final Any packed = toAny(messageOrAny);
        final Event result = Event.newBuilder()
                                  .setId(id)
                                  .setMessage(packed)
                                  .setContext(context)
                                  .build();
        return result;
    }

    /**
     * Creates an event based on the passed integration event.
     */
    public static Event toEvent(IntegrationEvent integrationEvent) {
        final IntegrationEventContext sourceContext = integrationEvent.getContext();
        final EventContext context = toEventContext(sourceContext);
        final Event result = createEvent(sourceContext.getEventId(),
                                         integrationEvent.getMessage(),
                                         context);
        return result;
    }

    private static EventContext toEventContext(IntegrationEventContext value) {
        final StringValue producerId = Wrapper.forString(value.getBoundedContextName());
        final Timestamp timestamp = value.getTimestamp();
        final Any producerAny = pack(producerId);
        return EventContext.newBuilder()
                           .setTimestamp(timestamp)
                           .setProducerId(producerAny)
                           .build();
    }

    private static EventContext createContext(Any producerId,
                                              CommandContext commandContext,
                                              @Nullable Version version) {
        final Timestamp timestamp = getCurrentTime();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setTimestamp(timestamp)
                                                         .setCommandContext(commandContext)
                                                         .setProducerId(producerId);
        if (version != null) {
            builder.setVersion(version);
        }
        return builder.build();
    }

    /**
     * Creates new builder for a factory.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds an event factory.
     */
    public static class Builder {

        private Any producerId;
        private CommandId commandId;
        private CommandContext commandContext;
        private int maxEventCount = EventIdSequence.MAX_ONE_DIGIT_SIZE;

        private Builder() {
            // Prevent instantiation from outside.
        }

        /**
         * Sets the ID of an entity which is producing the events wrapped into {@code Any}.
         */
        public Builder setProducerId(Message messageOrAny) {
            this.producerId = toAny(checkNotNull(messageOrAny));
            return this;
        }

        /**
         * Sets the ID of the command which caused events we are about to generate.
         */
        public Builder setCommandId(CommandId commandId) {
            this.commandId = checkNotNull(commandId);
            return this;
        }

        /**
         * Sets the command in response to which we generate events.
         */
        public Builder setCommandContext(CommandContext commandContext) {
            checkNotNull(commandContext);
            checkArgument(isNotDefault(commandContext));
            this.commandContext = commandContext;
            return this;
        }

        /**
         * Sets the maximum count of events the factory may produce in response a command.
         *
         * <p>Set the maximum event count to have leading zeroes in the sequence part of identifiers
         * of events generated by the factory.
         *
         * <p>If a value is not set, the {@linkplain EventIdSequence#MAX_ONE_DIGIT_SIZE
         * default value} will be used.
         */
        public Builder setMaxEventCount(int maxEventCount) {
            this.maxEventCount = maxEventCount;
            return this;
        }

        public EventFactory build() {
            checkNotNull(producerId, "Producer ID must be set.");
            checkNotNull(commandId, "Command ID must be set.");
            checkNotNull(commandContext, "Command must be set.");

            final EventFactory result = new EventFactory(this);
            return result;
        }
    }
}
