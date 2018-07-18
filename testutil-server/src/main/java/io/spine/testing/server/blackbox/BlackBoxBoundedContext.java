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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.testing.server.TestEventFactory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.AcknowledgementsVerifier;
import io.spine.util.Exceptions;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static java.util.Collections.singletonList;

/**
 * Black Box Bounded Context is aimed at facilitating writing literate integration tests.
 *
 * <p>Using its API commands and events are sent to a Bounded Context. Their effect is afterwards
 * verified in using various verifiers (e.g. {@link AcknowledgementsVerifier acknowledgement verfier},
 * {@link EmittedEventsVerifier emitted events verifier}).
 *
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("ClassWithTooManyMethods")
@VisibleForTesting
public class BlackBoxBoundedContext {

    private static final String NOT_A_DOMAIN_EVENT_ERROR = 
            "The Black Box bounded context expects a domain event, not a Spine Event instance";
    private final BoundedContext boundedContext;
    private final TestActorRequestFactory requestFactory;
    private final TestEventFactory eventFactory;
    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final TenantId tenantId;
    private final MemoizingObserver<Ack> observer;

    private BlackBoxBoundedContext() {
        this.boundedContext = newBoundedContext();
        this.tenantId = newTenantId();
        this.requestFactory = requestFactory(tenantId);
        this.eventFactory = eventFactory(requestFactory);
        this.commandBus = boundedContext.getCommandBus();
        this.eventBus = boundedContext.getEventBus();
        this.observer = memoizingObserver();
    }

    /*
     * Utilities for instance initialization.
     ******************************************************************************/

    /**
     * Creates a new {@link io.spine.client.ActorRequestFactory actor request factory} for tests
     * with a provided tenant ID.
     *
     * @param tenantId an identifier of a tenant that is executing requests in this Bounded Context
     * @return a new request factory instance
     */
    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(BlackBoxBoundedContext.class, tenantId);
    }

    /**
     * Creates a new {@link io.spine.server.event.EventFactory event factory} for tests which uses
     * the actor and the origin from the provided {@link io.spine.client.ActorRequestFactory request factory}.
     *
     * @param requestFactory a request factory bearing the actor and able to provide an origin for
     *                       factory generated events
     * @return a new event factory instance
     */
    private static TestEventFactory eventFactory(TestActorRequestFactory requestFactory) {
        return TestEventFactory.newInstance(requestFactory);
    }

    /**
     * @return a new {@link TenantId Tenant ID} with a random UUID value convenient
     * for test purposes.
     */
    private static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    /**
     * @return a new multitenant bounded context
     */
    private static BoundedContext newBoundedContext() {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .build();
    }

    /*
     * Methods populating the bounded context with repositories.
     ******************************************************************************/

    /**
     * Creates a new {@link BlackBoxBoundedContext black box Bounded Context} with provided
     * repositories.
     *
     * @param repositories repositories to register in the bounded context
     * @param <I>          the type of IDs used in the repository
     * @param <E>          the type of entities or aggregates
     * @return a newly created {@link BlackBoxBoundedContext Bounded Context black box}
     */
    @SafeVarargs
    public static <I, E extends Entity<I, ?>> BlackBoxBoundedContext
    with(Repository<I, E>... repositories) {
        BlackBoxBoundedContext blackBox = new BlackBoxBoundedContext();
        for (Repository<I, E> repository : repositories) {
            blackBox.boundedContext.register(repository);
        }
        return blackBox;
    }

    /**
     * Registers the provided repositories with the Bounded Context.
     *
     * @param repositories repositories to register in the bounded context
     * @param <I>          the type of IDs used in the repository
     * @param <E>          the type of entities or aggregates
     * @return current {@link BlackBoxBoundedContext} instance
     */
    @SafeVarargs
    public final <I, E extends Entity<I, ?>> BlackBoxBoundedContext
    andWith(Repository<I, E> firstRepository, Repository<I, E>... repositories) {
        checkNotNull(firstRepository);
        boundedContext.register(firstRepository);
        for (Repository<I, E> repository : repositories) {
            boundedContext.register(repository);
        }
        return this;
    }

    /*
     * Methods sending commands to the bounded context.
     ******************************************************************************/

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommand a domain command to be dispatched to the Bounded Context
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    public BlackBoxBoundedContext receivesCommand(Message domainCommand) {
        return this.receivesCommands(singletonList(domainCommand));
    }

    /**
     * Sends off provided commands to the Bounded Context.
     *
     * @param firstCommand  a domain command to be dispatched to the Bounded Context first
     * @param secondCommand a domain command to be dispatched to the Bounded Context second
     * @param otherCommands optional domain commands to be dispatched to the Bounded Context
     *                      in supplied order
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    public BlackBoxBoundedContext
    receivesCommands(Message firstCommand, Message secondCommand, Message... otherCommands) {
        return this.receivesCommands(asList(firstCommand, secondCommand, otherCommands));
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommands a list of domain commands to be dispatched to the Bounded Context
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    private BlackBoxBoundedContext receivesCommands(Collection<Message> domainCommands) {
        List<Command> commands = newArrayListWithCapacity(domainCommands.size());
        for (Message domainCommand : domainCommands) {
            commands.add(command(domainCommand));
        }
        commandBus.post(commands, observer);
        return this;
    }

    /**
     * Wraps the provided domain command message in a {@link Event Spine command}.
     *
     * @param commandMessage a domain command message
     * @return a newly created command instance
     */
    private Command command(Message commandMessage) {
        return requestFactory.command()
                             .create(commandMessage);
    }

    /*
     * Methods sending events to the bounded context.
     ******************************************************************************/

    /**
     * Sends off a provided event to the Bounded Context.
     *
     * @param domainEvent a domain event to be dispatched to the Bounded Context
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    public BlackBoxBoundedContext receivesEvent(Message domainEvent) {
        checkArgument(!(domainEvent instanceof Event), NOT_A_DOMAIN_EVENT_ERROR);
        return this.receivesEvents(singletonList(domainEvent));
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param firstEvent  a domain event to be dispatched to the Bounded Context first
     * @param secondEvent a domain event to be dispatched to the Bounded Context second
     * @param otherEvents optional domain events to be dispatched to the Bounded Context
     *                    in supplied order
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    public BlackBoxBoundedContext
    receivesEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
        checkArgument(!(firstEvent instanceof Event), NOT_A_DOMAIN_EVENT_ERROR);
        checkArgument(!(secondEvent instanceof Event), NOT_A_DOMAIN_EVENT_ERROR);
        return this.receivesEvents(asList(firstEvent, secondEvent, otherEvents));
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param domainEvents a list of domain event to be dispatched to the Bounded Context
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    private BlackBoxBoundedContext receivesEvents(Collection<Message> domainEvents) {
        List<Event> events = newArrayListWithCapacity(domainEvents.size());
        for (Message domainEvent : domainEvents) {
            events.add(event(domainEvent));
        }
        eventBus.post(events, observer);
        return this;
    }

    /**
     * Wraps the provided domain event message in a {@link Event Spine event}.
     *
     * @param eventMessage a domain event message
     * @return a newly created command instance
     */
    private Event event(Message eventMessage) {
        return eventFactory.createEvent(eventMessage);
    }

    /*
     * Methods verifying the bounded context behaviour.
     ******************************************************************************/

    /**
     * Executes the provided verifier, which throws an assertion error in case of unexpected results.
     *
     * @param verifier a verifier that checks the events emitted in this Bounded Context
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext verifiesThat(EmittedEventsVerifier verifier) {
        EmittedEvents events = emittedEvents();
        verifier.verify(events);
        return this;
    }

    private EmittedEvents emittedEvents() {
        List<Event> events = readAllEvents();
        return new EmittedEvents(events);
    }

    /**
     * Executes the provided verifier, which throws an assertion error in case of unexpected results.
     *
     * @param verifier a verifier that checks the acknowledgements in this Bounded Context
     * @return current {@link BlackBoxBoundedContext black box} instance
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext verifiesThat(AcknowledgementsVerifier verifier) {
        Acknowledgements acks = commandAcks();
        verifier.verify(acks);
        return this;
    }

    private Acknowledgements commandAcks() {
        return new Acknowledgements(observer.responses());
    }

    /*
     * Methods reading the events which were emitted in the bounded context.
     ******************************************************************************/

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    private List<Event> readAllEvents() {
        MemoizingObserver<Event> queryObserver = memoizingObserver();
        TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                eventBus.getEventStore()
                        .read(allEventsQuery(), queryObserver);
            }
        };
        operation.execute();

        List<Event> responses = queryObserver.responses();
        return responses;
    }

    /**
     * @return a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }


    /*
     * Bounded context lifecycle.
     ******************************************************************************/

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
            throw Exceptions.illegalStateWithCauseOf(e);
        }
    }
}
