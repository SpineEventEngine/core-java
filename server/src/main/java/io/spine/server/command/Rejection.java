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

package io.spine.server.command;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.server.event.EventFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * A wrapper of a rejection event.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class Rejection {

    private static final Any DEFAULT_PRODUCER_ID = Identifier.pack("Unknown");

    private final Event event;

    private Rejection(Event event) {
        this.event = event;
    }

    /**
     * Creates an instance of {@code Rejection} from the rejected command and a {@link Throwable}
     * caused by the {@link ThrowableMessage}.
     *
     * @param origin    the rejected command
     * @param throwable the caught error
     * @return new instance of {@code Rejection}
     * @throws IllegalArgumentException if the given {@link Throwable} is not caused by
     *                                  a {@link ThrowableMessage}
     */
    public static Rejection fromThrowable(CommandEnvelope origin, Throwable throwable) {
        checkNotNull(origin);
        checkNotNull(throwable);

        ThrowableMessage throwableMessage = unwrap(throwable);
        return from(origin, throwableMessage);
    }

    private static ThrowableMessage unwrap(Throwable causedByRejection) {
        Throwable cause = getRootCause(causedByRejection);
        boolean correctType = cause instanceof ThrowableMessage;
        checkArgument(correctType);
        ThrowableMessage throwableMessage = (ThrowableMessage) cause;
        return throwableMessage;
    }

    /**
     * Creates an instance of {@code Rejection} from the rejected command and
     * the {@link ThrowableMessage}.
     *
     * @param origin           the rejected command
     * @param throwableMessage the {@link ThrowableMessage} representing the command rejection
     * @return new instance of {@code Rejection}
     */
    public static Rejection from(CommandEnvelope origin, ThrowableMessage throwableMessage) {
        checkNotNull(origin);
        checkNotNull(throwableMessage);

        Event rejectionEvent = produceEvent(origin, throwableMessage);
        return new Rejection(rejectionEvent);
    }

    private static Event produceEvent(CommandEnvelope origin, ThrowableMessage message) {
        Any producerId = message.producerId()
                                .orElse(DEFAULT_PRODUCER_ID);
        EventFactory factory = EventFactory.on(origin, producerId);
        Message thrownMessage = message.getMessageThrown();
        RejectionEventContext context = context(origin.getMessage(), message);
        Event rejectionEvent = factory.createRejectionEvent(thrownMessage, null, context);
        return rejectionEvent;
    }

    /**
     * Obtains the wrapped event.
     *
     * @return rejection event represented by this {@code Rejection}
     */
    public Event asEvent() {
        return event;
    }

    /**
     * Obtains the wrapped event as an {@link EventEnvelope}.
     *
     * @return rejection event represented by this {@code Rejection}
     */
    public EventEnvelope asEnvelope() {
        return EventEnvelope.of(event);
    }

    /**
     * Obtains the message of the rejected command.
     *
     * @return the origin command
     */
    public Message origin() {
        Any commandMessageAny = event.getContext()
                                     .getRejection()
                                     .getCommandMessage();
        Message commandMessage = unpack(commandMessageAny);
        return commandMessage;
    }

    /**
     * Constructs a new {@link RejectionEventContext} from the given command message and
     * {@link ThrowableMessage}.
     *
     * @param commandMessage   rejected command
     * @param throwableMessage thrown rejection
     * @return new instance of {@code RejectionEventContext}
     */
    public static RejectionEventContext context(Message commandMessage,
                                                ThrowableMessage throwableMessage) {
        checkNotNull(commandMessage);
        checkNotNull(throwableMessage);

        String stacktrace = getStackTraceAsString(throwableMessage);
        return RejectionEventContext.newBuilder()
                                    .setCommandMessage(pack(commandMessage))
                                    .setStacktrace(stacktrace)
                                    .build();
    }

    /**
     * Checks if the given {@link Throwable} is caused by a rejection.
     *
     * <p>Returns {@code true} iff the root cause of {@code throwable} is an instance of
     * {@link ThrowableMessage}.
     *
     * @param throwable {@link Throwable} to check
     * @return {@code true} if {@code throwable} is caused by a rejection, {@code false} otherwise
     */
    public static boolean causedByRejection(Throwable throwable) {
        Throwable rootCause = Throwables.getRootCause(throwable);
        boolean result = rootCause instanceof ThrowableMessage;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rejection rejection = (Rejection) o;
        return Objects.equal(event, rejection.event);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(event);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("event", event)
                          .toString();
    }
}
