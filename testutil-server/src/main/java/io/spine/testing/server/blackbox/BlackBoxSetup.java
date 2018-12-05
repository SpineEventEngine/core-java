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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.ImportBus;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.integration.IntegrationBus;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static java.util.stream.Collectors.toList;

/**
 * A class which sets up a {@link BlackBoxBoundedContext}.
 *
 * <p>The setup may involve:
 * <ul>
 *     <li>posting of commands;
 *     <li>posting of events;
 *     <li>importing of events.
 * </ul>
 */
@VisibleForTesting
final class BlackBoxSetup {

    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final ImportBus importBus;
    private final TestActorRequestFactory requestFactory;
    private final TestEventFactory eventFactory;
    private final IntegrationBus integrationBus;
    private final MemoizingObserver<Ack> observer;

    BlackBoxSetup(BoundedContext boundedContext,
                  TestActorRequestFactory requestFactory,
                  MemoizingObserver<Ack> observer) {
        this.commandBus = boundedContext.getCommandBus();
        this.eventBus = boundedContext.getEventBus();
        this.importBus = boundedContext.getImportBus();
        this.integrationBus = boundedContext.getIntegrationBus();
        this.requestFactory = checkNotNull(requestFactory);
        this.eventFactory = eventFactory(requestFactory);
        this.observer = checkNotNull(observer);
    }

    /**
     * Posts commands to the bounded context.
     *
     * @param domainCommands
     *         a list of {@linkplain CommandMessage command messages}
     *         or {@linkplain Command commands}
     */
    void postCommands(Collection<Message> domainCommands) {
        List<Command> commands = domainCommands.stream()
                                               .map(commandOrMessage -> command(commandOrMessage,
                                                                                requestFactory))
                                               .collect(toList());
        commandBus.post(commands, observer);
    }

    /**
     * Posts events to the bounded context.
     *
     * @param domainEvents
     *         a list of {@linkplain EventMessage event messages} or {@linkplain Event events}
     * @return list of events posted to {@link EventBus}
     */
    List<Event> postEvents(Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents, eventFactory);
        eventBus.post(events, observer);
        return events;
    }

    /**
     * Posts events with the specified producer to the bounded context.
     *
     * @param producerId
     *         the {@linkplain io.spine.core.EventContext#getProducerId() producer} for events
     * @param firstEvent
     *         the first event to be posted
     * @param otherEvents
     *         other events to be posted, if any
     * @return list of events posted to {@link EventBus}
     */
    List<Event> postEvents(Object producerId, EventMessage firstEvent,
                           EventMessage... otherEvents) {
        List<Message> eventMessages = asList(firstEvent, otherEvents);
        TestEventFactory customFactory = newEventFactory(producerId);
        List<Event> events = toEvents(eventMessages, customFactory);
        eventBus.post(events, observer);
        return events;
    }

    /**
     * Posts events to the bounded context.
     *
     * @param sourceContext
     *         a name of a Bounded Context external events come from
     * @param domainEvents
     *         a list of {@linkplain EventMessage event messages} or {@linkplain Event events}
     */
    void postExternalEvents(BoundedContextName sourceContext, Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents, eventFactory);
        List<ExternalMessage> externalEvents = events
                .stream()
                .map(event -> ExternalMessages.of(event, sourceContext))
                .collect(toList());
        integrationBus.post(externalEvents, observer);
    }

    /**
     * {@linkplain ImportBus Imports} events to the bounded context.
     *
     * @param domainEvents
     *         a list of events to import
     */
    void importEvents(Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents, eventFactory);
        importBus.post(events, observer);
    }

    private static List<Event> toEvents(Collection<Message> domainEvents,
                                        TestEventFactory eventFactory) {
        return domainEvents
                .stream()
                .map(domainEvent -> event(domainEvent, eventFactory))
                .collect(toList());
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

    /**
     * Generates a {@link Command} from a {@link CommandMessage}.
     *
     * <p>If the passed message is a {@link Command} does nothing.
     *
     * @param commandOrMessage
     *         a command or command message
     * @param requestFactory
     *         the factory to produce commands with
     * @return a {@code Command}
     */
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
     * Creates a new instance of {@link TestEventFactory} which supplies the passed value
     * of the {@linkplain io.spine.core.EventContext#getProducerId() event producer ID}.
     *
     * @param producerId
     *         can be {@code Integer}, {@code Long}, {@link String}, or {@code Message}
     */
    private TestEventFactory newEventFactory(Object producerId) {
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
