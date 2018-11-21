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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.ImportBus;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.stream.Collectors.toList;

/**
 * An input of a {@link BlackBoxBoundedContext}, which receives domain messages.
 */
@VisibleForTesting
final class BlackBoxInput {

    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final ImportBus importBus;
    private final TestActorRequestFactory requestFactory;
    private final TestEventFactory eventFactory;
    private final MemoizingObserver<Ack> observer;

    BlackBoxInput(BoundedContext boundedContext,
                  TestActorRequestFactory requestFactory,
                  MemoizingObserver<Ack> observer) {
        this.commandBus = boundedContext.getCommandBus();
        this.eventBus = boundedContext.getEventBus();
        this.importBus = boundedContext.getImportBus();
        this.requestFactory = checkNotNull(requestFactory);
        this.eventFactory = eventFactory(requestFactory);
        this.observer = checkNotNull(observer);
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommands
     *         a list of domain commands to be dispatched to the Bounded Context
     */
    void receivesCommands(Collection<Message> domainCommands) {
        List<Command> commands = domainCommands.stream()
                                               .map(commandOrMessage -> command(commandOrMessage,
                                                                                requestFactory))
                                               .collect(toList());
        commandBus.post(commands, observer);
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param domainEvents
     *         a list of domain event to be dispatched to the Bounded Context
     */
    void receivesEvents(Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents);
        eventBus.post(events, observer);
    }

    void importsEvents(Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents);
        importBus.post(events, observer);
    }

    private List<Event> toEvents(Collection<Message> domainEvents) {
        List<Event> events = newArrayListWithCapacity(domainEvents.size());
        for (Message domainEvent : domainEvents) {
            events.add(event(domainEvent, eventFactory));
        }
        return events;
    }

    /**
     * Generates {@link Event} with the passed instance is an event message. If the passed
     * instance is {@code Event} returns it.
     *
     * @param eventOrMessage
     *         a domain event message or {@code Event}
     * @param eventFactory
     *         the event factory to produce events with
     * @return a newly created {@code Event} instance or passed {@code Event}
     */
    private static Event event(Message eventOrMessage, TestEventFactory eventFactory) {
        if (eventOrMessage instanceof Event) {
            return (Event) eventOrMessage;
        }
        EventMessage message = (EventMessage) eventOrMessage;
        return eventFactory.createEvent(message);
    }

    private static Command command(Message commandOrMessage,
                                   TestActorRequestFactory requestFactory) {
        if (commandOrMessage instanceof Command) {
            return (Command) commandOrMessage;
        }
        CommandMessage message = (CommandMessage) commandOrMessage;
        return requestFactory.command()
                             .create(message);
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
