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
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.event.Enricher;
import io.spine.server.event.EventBus;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.singletonList;

/**
 * Black Box Bounded Context is aimed at facilitating writing literate integration tests.
 *
 * <p>Using its API commands and events are sent to a Bounded Context. Their effect is afterwards
 * verified in using various verifiers (e.g. {@link io.spine.testing.server.blackbox.verify.state.VerifyState
 * state verfier}, {@link VerifyEvents emitted events verifier}).
 *
 * @param <T> the type of the bounded context descendant
 */
@VisibleForTesting
public abstract class BlackBoxBoundedContext<T extends BlackBoxBoundedContext> {

    private final BoundedContext boundedContext;
    private final TestActorRequestFactory requestFactory;
    private final BlackBoxInput input;
    private final BlackBoxOutput output;

    protected BlackBoxBoundedContext(boolean multitenant,
                                     Enricher enricher,
                                     TestActorRequestFactory requestFactory) {
        CommandMemoizingTap commandTap = new CommandMemoizingTap();
        this.boundedContext = BoundedContext
                .newBuilder()
                .setMultitenant(multitenant)
                .setCommandBus(CommandBus.newBuilder()
                                         .appendFilter(commandTap))
                .setEventBus(EventBus.newBuilder()
                                     .setEnricher(enricher))
                .build();
        MemoizingObserver<Ack> observer = memoizingObserver();
        this.requestFactory = requestFactory;
        this.input = new BlackBoxInput(boundedContext, requestFactory, observer);
        this.output = new BlackBoxOutput(boundedContext, commandTap, observer);
    }

    /**
     * Registers passed repositories with the Bounded Context.
     *
     * @param repositories
     *         repositories to register in the Bounded Context
     * @return current instance
     */
    public final T with(Repository<?, ?>... repositories) {
        checkNotNull(repositories);
        for (Repository<?, ?> repository : repositories) {
            checkNotNull(repository);
            boundedContext.register(repository);
        }
        return thisRef();
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommand
     *         a domain command to be dispatched to the Bounded Context
     * @return current instance
     */
    public T receivesCommand(Message domainCommand) {
        return this.receivesCommands(singletonList(domainCommand));
    }

    /**
     * Sends off provided commands to the Bounded Context.
     *
     * @param firstCommand
     *         a domain command to be dispatched to the Bounded Context first
     * @param secondCommand
     *         a domain command to be dispatched to the Bounded Context second
     * @param otherCommands
     *         optional domain commands to be dispatched to the Bounded Context
     *         in supplied order
     * @return current instance
     */
    public T
    receivesCommands(Message firstCommand, Message secondCommand, Message... otherCommands) {
        return this.receivesCommands(asList(firstCommand, secondCommand, otherCommands));
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommands
     *         a list of domain commands to be dispatched to the Bounded Context
     * @return current instance
     */
    private T receivesCommands(Collection<Message> domainCommands) {
        input.receivesCommands(domainCommands);
        return thisRef();
    }

    /**
     * Sends off a provided event to the Bounded Context.
     *
     * @param messageOrEvent
     *         an event message or {@link io.spine.core.Event}. If an instance of {@code Event} is passed, it
     *         will be posted to {@link EventBus} as is.
     *         Otherwise, an instance of {@code Event} will be generated basing on the passed
     *         event message and posted to the bus.
     * @return current instance
     */
    public T receivesEvent(Message messageOrEvent) {
        return this.receivesEvents(singletonList(messageOrEvent));
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * <p>The method accepts event messages or instances of {@link io.spine.core.Event}.
     * If an instance of {@code Event} is passed, it will be posted to {@link EventBus} as is.
     * Otherwise, an instance of {@code Event} will be generated basing on the passed event
     * message and posted to the bus.
     *
     * @param firstEvent
     *         a domain event to be dispatched to the Bounded Context first
     * @param secondEvent
     *         a domain event to be dispatched to the Bounded Context second
     * @param otherEvents
     *         optional domain events to be dispatched to the Bounded Context
     *         in supplied order
     * @return current instance
     */
    @SuppressWarnings("unused")
    public T
    receivesEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
        return this.receivesEvents(asList(firstEvent, secondEvent, otherEvents));
    }

    /**
     * Sends off events using the specified producer to the Bounded Context.
     *
     * <p>The method is needed to route events based on a proper producer ID.
     *
     * @param producerId
     *         the {@linkplain io.spine.core.EventContext#getProducerId() producer} for events
     * @param firstEvent
     *         a domain event to be dispatched to the Bounded Context first
     * @param otherEvents
     *         optional domain events to be dispatched to the Bounded Context
     *         in supplied order
     * @return current instance
     */
    public T
    receivesEventsProducedBy(Object producerId,
                             EventMessage firstEvent, EventMessage... otherEvents) {
        List<EventMessage> eventMessages = asList(firstEvent, otherEvents);
        TestEventFactory customFactory = newEventFactory(producerId);
        List<Message> events = eventMessages.stream()
                                            .map(customFactory::createEvent)
                                            .collect(Collectors.toList());
        return this.receivesEvents(events);
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param domainEvents
     *         a list of domain event to be dispatched to the Bounded Context
     * @return current instance
     */
    private T receivesEvents(Collection<Message> domainEvents) {
        input.receivesEvents(domainEvents);
        return thisRef();
    }

    public T importsEvent(Message eventOrMessage) {
        return this.importAll(singletonList(eventOrMessage));
    }

    public T
    importsEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
        return this.importAll(asList(firstEvent, secondEvent, otherEvents));
    }

    private T importAll(Collection<Message> domainEvents) {
        input.importsEvents(domainEvents);
        return thisRef();
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
     * Closes the bounded context so that it shutting down all of its repositories.
     *
     * <p>Instead of a checked {@link java.io.IOException IOException}, wraps any issues
     * that may occur while closing, into an {@link IllegalStateException}.
     */
    public void close() {
        try {
            boundedContext.close();
        } catch (Exception e) {
            throw illegalStateWithCauseOf(e);
        }
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

    /** Casts this to generic type to provide type covariance in the derived classes. */
    @SuppressWarnings("unchecked" /* See Javadoc. */)
    private T thisRef() {
        return (T) this;
    }

    protected BlackBoxOutput output() {
        return output;
    }
}
