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

package io.spine.testing.server.blackbox;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayListWithCapacity;

/**
 * Black Box Bounded Context is aimed at facilitating writing literate integration tests.
 *
 * <p>Using its API commands and events are sent to a Bounded Context. Their effect is afterwards
 * verified in using various verifiers (e.g. {@link io.spine.testing.server.blackbox.verify.state.VerifyState
 * state verfier}, {@link VerifyEvents emitted events verifier}).
 */
public class AbstractBlackBoxContext {

    private final TestActorRequestFactory requestFactory;
    private final TestEventFactory eventFactory;

    protected AbstractBlackBoxContext(TestActorRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
        this.eventFactory = eventFactory(requestFactory);
    }

    protected List<Event> toEvents(Collection<Message> domainEvents) {
        List<Event> events = newArrayListWithCapacity(domainEvents.size());
        for (Message domainEvent : domainEvents) {
            events.add(event(domainEvent));
        }
        return events;
    }

    /**
     * Generates {@link Event} with the passed instance is an event message. If the passed
     * instance is {@code Event} returns it.
     *
     * @param eventOrMessage
     *         a domain event message or {@code Event}
     * @return a newly created {@code Event} instance or passed {@code Event}
     */
    private Event event(Message eventOrMessage) {
        if (eventOrMessage instanceof Event) {
            return (Event) eventOrMessage;
        }
        EventMessage message = (EventMessage) eventOrMessage;
        return eventFactory.createEvent(message);
    }

    protected Command command(Message commandOrMessage) {
        if (commandOrMessage instanceof Command) {
            return (Command) commandOrMessage;
        }
        CommandMessage message = (CommandMessage) commandOrMessage;
        return requestFactory.command()
                             .create(message);
    }

    /**
     * Creates a new instance of {@link TestEventFactory} which supplies mock
     * for {@linkplain io.spine.core.EventContext#getProducerId() producer ID} values.
     */
    public TestEventFactory newEventFactory() {
        return eventFactory(requestFactory);
    }

    /**
     * Creates a new instance of {@link TestEventFactory} which supplies the passed value
     * of the {@linkplain io.spine.core.EventContext#getProducerId() event producer ID}.
     *
     * @param producerId
     *         can be {@code Integer}, {@code Long}, {@link String}, or {@code Message}
     */
    public TestEventFactory newEventFactory(Object producerId) {
        checkNotNull(producerId);
        Any id = producerId instanceof Any
                 ? (Any) producerId
                 : Identifier.pack(producerId);
        return TestEventFactory.newInstance(id, requestFactory);
    }

    /**
     * Creates a new {@link io.spine.server.event.EventFactory event factory} for tests which uses
     * the actor and the origin from the provided {@link io.spine.client.ActorRequestFactory
     * request factory}.
     *
     * @param requestFactory
     *         a request factory bearing the actor and able to provide an origin for
     *         factory generated events
     * @return a new event factory instance
     */
    private static TestEventFactory eventFactory(TestActorRequestFactory requestFactory) {
        return TestEventFactory.newInstance(requestFactory);
    }
}
