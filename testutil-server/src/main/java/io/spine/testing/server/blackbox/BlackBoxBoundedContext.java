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
import io.spine.base.EventMessage;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.QueryService;
import io.spine.server.aggregate.ImportBus;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.VerifyState.VerifyStateByTenant;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * A black box bounded context for writing integration tests in multitenant environment.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
@VisibleForTesting
public class BlackBoxBoundedContext extends AbstractBlackBoxContext {

    private final BoundedContext boundedContext;
    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final ImportBus importBus;
    private final TenantId tenantId;
    private final MemoizingObserver<Ack> observer;
    private final CommandMemoizingTap commandTap;

    /**
     * Creates a new multi-tenant instance.
     */
    BlackBoxBoundedContext(BlackBoxBuilder builder) {
        super(requestFactory(builder.buildTenant()));
        this.commandTap = new CommandMemoizingTap();
        this.boundedContext = BoundedContext
                .newBuilder()
                .setMultitenant(true)
                .setCommandBus(CommandBus.newBuilder()
                                         .appendFilter(commandTap))
                .setEventBus(EventBus.newBuilder()
                                     .setEnricher(builder.buildEnricher()))
                .build();
        this.tenantId = builder.buildTenant();
        this.commandBus = boundedContext.getCommandBus();
        this.eventBus = boundedContext.getEventBus();
        this.importBus = boundedContext.getImportBus();
        this.observer = memoizingObserver();
    }

    /**
     * Creates a new bounded context with the default configuration.
     */
    public static BlackBoxBoundedContext newInstance() {
        return newBuilder().build();
    }

    /**
     * Creates a builder for a black box bounded context.
     */
    public static BlackBoxBuilder newBuilder() {
        return new BlackBoxBuilder();
    }

    /*
     * Utilities for instance initialization.
     ******************************************************************************/

    /**
     * Creates a new {@link io.spine.client.ActorRequestFactory actor request factory} for tests
     * with a provided tenant ID.
     *
     * @param tenantId
     *         an identifier of a tenant that is executing requests in this Bounded Context
     * @return a new request factory instance
     */
    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(BlackBoxBoundedContext.class, tenantId);
    }

    /*
     * Methods populating the bounded context with repositories.
     ******************************************************************************/

    /**
     * Registers passed repositories with the Bounded Context.
     *
     * @param repositories
     *         repositories to register in the Bounded Context
     * @return current instance
     */
    public final BlackBoxBoundedContext with(Repository<?, ?>... repositories) {
        checkNotNull(repositories);
        for (Repository<?, ?> repository : repositories) {
            checkNotNull(repository);
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
     * @param domainCommand
     *         a domain command to be dispatched to the Bounded Context
     * @return current instance
     */
    public BlackBoxBoundedContext receivesCommand(Message domainCommand) {
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
    public BlackBoxBoundedContext
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
    private BlackBoxBoundedContext receivesCommands(Collection<Message> domainCommands) {
        List<Command> commands = domainCommands.stream()
                                               .map(this::command)
                                               .collect(toList());
        commandBus.post(commands, observer);
        return this;
    }

    /*
     * Methods sending events to the bounded context.
     ******************************************************************************/

    /**
     * Sends off a provided event to the Bounded Context.
     *
     * @param messageOrEvent
     *         an event message or {@link Event}. If an instance of {@code Event} is passed, it
     *         will be posted to {@link EventBus} as is.
     *         Otherwise, an instance of {@code Event} will be generated basing on the passed
     *         event message and posted to the bus.
     * @return current instance
     */
    public BlackBoxBoundedContext receivesEvent(Message messageOrEvent) {
        return this.receivesEvents(singletonList(messageOrEvent));
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * <p>The method accepts event messages or instances of {@link Event}.
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
    public BlackBoxBoundedContext
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
    public BlackBoxBoundedContext
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
    private BlackBoxBoundedContext receivesEvents(Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents);
        eventBus.post(events, observer);
        return this;
    }

    public BlackBoxBoundedContext importsEvent(Message eventOrMessage) {
        return this.importAll(singletonList(eventOrMessage));
    }

    public BlackBoxBoundedContext
    importsEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
        return this.importAll(asList(firstEvent, secondEvent, otherEvents));
    }

    private BlackBoxBoundedContext importAll(Collection<Message> domainEvents) {
        List<Event> events = toEvents(domainEvents);
        importBus.post(events, observer);
        return this;
    }

    /*
     * Methods verifying the bounded context behaviour.
     ******************************************************************************/

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext assertThat(VerifyEvents verifier) {
        EmittedEvents events = emittedEvents();
        verifier.verify(events);
        return this;
    }

    private EmittedEvents emittedEvents() {
        List<Event> events = readAllEvents();
        return new EmittedEvents(events);
    }

    /**
     * Executes the provided verifier, which throws an assertion error in case of
     * unexpected results.
     *
     * @param verifier
     *         a verifier that checks the acknowledgements in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext assertThat(VerifyAcknowledgements verifier) {
        Acknowledgements acks = commandAcks();
        verifier.verify(acks);
        return this;
    }

    /**
     * Verifies emitted commands by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the commands emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext assertThat(VerifyCommands verifier) {
        EmittedCommands commands = emittedCommands();
        verifier.verify(commands);
        return this;
    }

    /**
     * Does the same as {@link #assertThat(VerifyStateByTenant)}, but with a custom tenant ID.
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext assertThat(VerifyState verifier) {
        QueryService queryService = QueryService
                .newBuilder()
                .add(boundedContext)
                .build();
        verifier.verify(queryService);
        return this;
    }

    /**
     * Asserts the state of an entity using the {@link #tenantId}.
     *
     * @param verifyByTenant
     *         the function to produce {@link VerifyState} with specific tenant ID
     * @return current instance
     */
    @CanIgnoreReturnValue
    public BlackBoxBoundedContext assertThat(VerifyStateByTenant verifyByTenant) {
        VerifyState verifier = verifyByTenant.apply(tenantId);
        assertThat(verifier);
        return this;
    }

    private EmittedCommands emittedCommands() {
        List<Command> commands = readAllCommands();
        return new EmittedCommands(commands);
    }

    private List<Command> readAllCommands() {
        return commandTap.commands();
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
     * Creates a new {@link EventStreamQuery} without any filters.
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
            throw illegalStateWithCauseOf(e);
        }
    }
}
