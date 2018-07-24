/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.command.dispatch;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.core.Version;
import io.spine.server.event.EventFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toList;

/**
 * The events emitted as a result of message dispatch.
 *
 * <p>Dispatch result can be treated in different forms.
 * (e.g. {@link #asMessages() as messages} or {@link #asEvents(Any, Version) as events}).
 *
 * @author Mykhailo Drachuk
 * @author Dmytro Dashenkov
 */
public final class DispatchResult {

    private final MessageEnvelope origin;
    private final ImmutableList<? extends Message> messages;
    private final boolean rejection;
    private final @Nullable RejectionEventContext rejectionContext;

    private DispatchResult(ImmutableList<? extends Message> messages,
                           MessageEnvelope origin,
                           boolean rejection,
                           @Nullable RejectionEventContext rejectionContext) {
        this.messages = messages;
        this.origin = origin;
        this.rejection = rejection;
        this.rejectionContext = rejectionContext;
    }

    /**
     * Creates a new {@code DispatchResult} from the given event messages.
     *
     * @param messages event messages which were emitted by the dispatch
     * @param origin   a message that was dispatched
     */
    static DispatchResult ofEvents(List<? extends Message> messages, MessageEnvelope origin) {
        checkNotNull(messages);
        checkNotNull(origin);

        return new DispatchResult(copyOf(messages), origin, false, null);
    }

    /**
     * Creates a new {@code DispatchResult} from the given rejection message.
     *
     * @param message rejection thrown by the dispatch
     * @param origin  a message that was dispatched
     */
    static DispatchResult ofRejection(Message message,
                                      CommandEnvelope origin,
                                      RejectionEventContext context) {
        checkNotNull(message);
        checkNotNull(origin);

        return new DispatchResult(of(message), origin, true, context);
    }

    /**
     * @return dispatch result representation as a list of domain event messages
     */
    public List<? extends Message> asMessages() {
        return copyOf(this.messages);
    }

    public DispatchResult filter(Predicate<? super Message> condition) {
        if (rejection) {
            return this;
        } else {
            ImmutableList<? extends Message> messages = this.messages.stream()
                                                                     .filter(condition)
                                                                     .collect(toImmutableList());
            return new DispatchResult(messages, origin, false, rejectionContext);
        }
    }

    /**
     * Obtains the emitted events as a list of {@link Event}.
     *
     * @return dispatch result representation as a list of events
     */
    public List<Event> asEvents(Any producerId, @Nullable Version version) {
        checkNotNull(producerId);
        List<Event> result =
                messages.stream()
                        .map(toEventFunction(producerId, version))
                        .collect(toList());
        return result;
    }

    private Function<Message, Event> toEventFunction(Any producerId, @Nullable Version version) {
        if (rejection) {
            checkNotNull(rejectionContext,
                         "Rejection %s was thrown but no %s supplied.",
                         messages.get(0));
            return new RejectionToEvent(producerId, version, rejectionContext);
        } else {
            return new EventToEvent(producerId, version);
        }
    }

    /**
     * The implementation base for a function converting an event message to an {@link Event}.
     */
    private abstract class ToEvent implements Function<Message, Event> {

        private final EventFactory eventFactory;
        private final @Nullable Version version;

        private ToEvent(Any producerId, @Nullable Version version) {
            this.eventFactory = EventFactory.on(DispatchResult.this.origin, producerId);
            this.version = version;
        }

        protected EventFactory eventFactory() {
            return eventFactory;
        }

        protected @Nullable Version version() {
            return version;
        }
    }

    /**
     * Converts an event message into an genuine {@link Event}.
     */
    private final class EventToEvent extends ToEvent {

        private EventToEvent(Any producerId, @Nullable Version version) {
            super(producerId, version);
        }

        @Override
        public Event apply(Message message) {
            return eventFactory().createEvent(message, version());
        }
    }

    /**
     * Converts a rejection message into a rejection {@linkplain Event event}.
     */
    private final class RejectionToEvent extends ToEvent {

        private final RejectionEventContext context;

        private RejectionToEvent(Any producerId,
                                 @Nullable Version version,
                                 RejectionEventContext rejectionContext) {
            super(producerId, version);
            this.context = rejectionContext;
        }

        @Override
        public Event apply(Message message) {
            return eventFactory().createRejectionEvent(message, version(), context);
        }
    }
}
