/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoFluentAssertion;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.client.Client;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Topic;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.grpc.MemoizingObserver;
import io.spine.logging.Logging;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.Closeable;
import io.spine.server.QueryService;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStore;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.CommandSubject;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.entity.EntitySubject;
import io.spine.testing.server.query.QueryResultSubject;
import io.spine.time.ZoneId;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.asList;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.entity.model.EntityClass.stateClassOf;
import static io.spine.testing.server.blackbox.Actor.defaultActor;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class provides means for integration testing of Bounded Contexts.
 *
 * <p>Such a test suite would send commands or events to the Bounded Context under the test,
 * and then verify consequences of handling a command or an event.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
@VisibleForTesting
public abstract class BlackBox implements Logging, Closeable {

    /**
     * The context under the test.
     */
    private final BoundedContext context;

    /**
     * A supplier of {@link Client}s which are linked to this context.
     */
    private final ClientSupplier clientSupplier;

    /**
     * Collects all commands, including posted to the context during its setup or
     * generated by the entities in response to posted or generated events or commands.
     */
    private final CommandCollector commands;

    /**
     * Commands received by this instance and posted to Command Bus during the test setup.
     *
     * <p>These commands are filtered out from those generated in the Bounded Context
     * (in response to posted or generated events or commands), which are used for assertions.
     */
    private final Set<Command> postedCommands;

    /**
     * Collects all events, including posted to the context during its setup or
     * generated by the entities in response to posted or generated events or commands.
     */
    private final EventCollector events;

    /**
     * Events received by this instance and posted to the Event Bus during the test setup.
     *
     * <p>These events are filtered out from those stored in the Bounded Context to
     * collect only the emitted events, which are used for assertions.
     */
    private final Set<Event> postedEvents;

    /**
     * Handles runtime exceptions thrown from signal handlers.
     */
    private final FailedHandlerGuard failedHandlerGuard;

    /**
     * Information about the current user and the time-zone.
     */
    private Actor actor;

    /**
     * Creates new instance obtaining configuration parameters from the passed builder.
     */
    public static BlackBox from(BoundedContextBuilder builder) {
        BlackBox result =
                builder.isMultitenant()
                ? new MtBlackBox(builder)
                : new StBlackBox(builder);
        return result;
    }

    BlackBox(BoundedContextBuilder builder) {
        super();
        this.commands = new CommandCollector();
        this.postedCommands = synchronizedSet(new HashSet<>());
        this.events = new EventCollector();
        this.postedEvents = synchronizedSet(new HashSet<>());
        this.failedHandlerGuard = new FailedHandlerGuard();
        BoundedContextBuilder wiredCopy = wiredCopyOf(builder);
        this.context = wiredCopy.build();
        this.actor = defaultActor();
        this.clientSupplier = new ClientSupplier(context);
    }

    private BoundedContextBuilder wiredCopyOf(BoundedContextBuilder builder) {
        BoundedContextBuilder result = builder.testingCopy();
        result.addCommandListener(commands)
              .addEventListener(events)
              .addEventDispatcher(failedHandlerGuard)
              .addEventDispatcher(new UnsupportedCommandGuard(result.name()
                                                                    .getValue()))
              .addEventDispatcher(DiagnosticLog.instance());
        return result;
    }

    /**
     * Obtains the name of the context under the test.
     */
    public BoundedContextName name() {
        return context.name();
    }

    /**
     * Makes all future requests to the context come to the tenant with the passed ID.
     *
     * @throws IllegalStateException
     *         if the method is called for a single-tenant context
     */
    public abstract BlackBox withTenant(TenantId tenant);

    /**
     * Sets the given {@link UserId} as the actor ID for the requests produced by this context.
     */
    public final BlackBox withActor(UserId user) {
        checkNotNull(user);
        actor = actor.withId(user);
        return this;
    }

    /**
     * Sets the given time zone parameters for the actor requests produced by this context.
     */
    public final BlackBox in(ZoneId zoneId) {
        checkNotNull(zoneId);
        actor = actor.in(zoneId);
        return this;
    }

    /**
     * Tells context to log signal handler failures over failing the test.
     */
    @CanIgnoreReturnValue
    public final BlackBox tolerateFailures() {
        failedHandlerGuard.tolerateFailures();
        return this;
    }

    /**
     * Obtains the current {@link Actor}.
     */
    final Actor actor() {
        return this.actor;
    }

    @VisibleForTesting
    final BoundedContext context() {
        return context;
    }

    /**
     * Appends the passed event to the history of the context under the test.
     */
    @CanIgnoreReturnValue
    public final BlackBox append(Event event) {
        checkNotNull(event);
        EventStore eventStore =
                context.eventBus()
                       .eventStore();
        eventStore.append(event);
        return this;
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommand
     *         a domain command to be dispatched to the Bounded Context
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public final BlackBox receivesCommand(CommandMessage domainCommand) {
        checkNotNull(domainCommand);
        return receivesCommands(singletonList(domainCommand));
    }

    /**
     * Sends off provided commands to the Bounded Context.
     *
     * @param first
     *         a domain command to be dispatched to the Bounded Context first
     * @param second
     *         a domain command to be dispatched to the Bounded Context second
     * @param rest
     *         optional domain commands to be dispatched to the Bounded Context in supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public final BlackBox
    receivesCommands(CommandMessage first, CommandMessage second, CommandMessage... rest) {
        checkNotNull(first);
        checkNotNull(second);
        return receivesCommands(asList(first, second, rest));
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommands
     *         a list of domain commands to be dispatched to the Bounded Context
     * @return current instance
     */
    private BlackBox receivesCommands(Collection<CommandMessage> domainCommands) {
        checkNotNull(domainCommands);
        List<Command> posted = setup().postCommands(domainCommands);
        postedCommands.addAll(posted);
        return this;
    }

    /**
     * Sends off a provided event to the Bounded Context.
     *
     * @param domainEvent
     *         a domain event to be dispatched to the Bounded Context.
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public final BlackBox receivesEvent(EventMessage domainEvent) {
        checkNotNull(domainEvent);
        return receivesEvents(singletonList(domainEvent));
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * <p>The method accepts event messages or instances of {@link io.spine.core.Event}.
     * If an instance of {@code Event} is passed, it will be posted to {@link EventBus} as is.
     * Otherwise, an instance of {@code Event} will be generated basing on the passed event
     * message and posted to the bus.
     *
     * @param first
     *         a domain event to be dispatched first
     * @param second
     *         a domain event to be dispatched second
     * @param rest
     *         optional domain events to be dispatched in the supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @SuppressWarnings("OverloadedVarargsMethod")    // Different number of mandatory args.
    @CanIgnoreReturnValue
    public final BlackBox
    receivesEvents(EventMessage first, EventMessage second, EventMessage... rest) {
        checkNotNull(first);
        checkNotNull(second);
        return receivesEvents(asList(first, second, rest));
    }

    /**
     * Sends off a provided event to the Bounded Context as event from an external source.
     *
     * @param messageOrEvent
     *         an event message or {@link Event}. If an instance of {@code Event} is
     *         passed, it will be posted to {@link io.spine.server.integration.IntegrationBroker
     *         IntegrationBroker} as-is.
     *         Otherwise, an instance of {@code Event} will be generated basing
     *         on the passed event message and posted to the bus.
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public final BlackBox receivesExternalEvent(Message messageOrEvent) {
        checkNotNull(messageOrEvent);
        setup().postExternalEvent(messageOrEvent);
        return this;
    }

    /**
     * Sends off provided events to the Bounded Context as events from an external source.
     *
     * <p>The method accepts event messages or instances of {@link io.spine.core.Event}.
     * If an instance of {@code Event} is passed, it will be posted to
     * {@link io.spine.server.integration.IntegrationBroker IntegrationBroker} as-is.
     * Otherwise, an instance of {@code Event} will be generated basing on the passed event
     * message and posted to the bus.
     *
     * @param first
     *         an external event to be dispatched first
     * @param second
     *         an external event to be dispatched second
     * @param other
     *         optional external events to be dispatched in the supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @SuppressWarnings("unused") // IDEA does not see the usage of this method from tests.
    @CanIgnoreReturnValue
    public final BlackBox
    receivesExternalEvents(EventMessage first, EventMessage second, EventMessage... other) {
        checkNotNull(first);
        checkNotNull(second);
        return receivesExternalEvents(asList(first, second, other));
    }

    /**
     * Sends off provided events to the Bounded Context as events from an external source.
     *
     * @param eventMessages
     *         a list of external events to be dispatched to the Bounded Context
     * @return current instance
     */
    private BlackBox receivesExternalEvents(Collection<EventMessage> eventMessages) {
        setup().postExternalEvents(eventMessages);
        return this;
    }

    /**
     * Sends off events using the specified producer to the Bounded Context.
     *
     * <p>The method is needed to route events based on a proper producer ID.
     *
     * @param producerId
     *         the {@linkplain io.spine.core.EventContext#getProducerId() producer} for events
     * @param first
     *         a domain event to be dispatched to the Bounded Context first
     * @param rest
     *         optional domain events to be dispatched to the Bounded Context in the supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public final BlackBox
    receivesEventsProducedBy(Object producerId, EventMessage first, EventMessage... rest) {
        List<Event> sentEvents = setup().postEvents(producerId, first, rest);
        postedEvents.addAll(sentEvents);
        return this;
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param domainEvents
     *         a list of domain event to be dispatched to the Bounded Context
     * @return current instance
     */
    private BlackBox receivesEvents(Collection<EventMessage> domainEvents) {
        List<Event> sentEvents = setup().postEvents(domainEvents);
        this.postedEvents.addAll(sentEvents);
        return this;
    }

    /**
     * Imports the event into the Bounded Context.
     *
     * @param eventOrMessage
     *         and event message or {@link Event} instance
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public BlackBox importsEvent(Message eventOrMessage) {
        setup().importEvent(eventOrMessage);
        return this;
    }

    /**
     * Imports passed events into the Bounded Context.
     *
     * @param first
     *         an event message or {@code Event} to be imported first
     * @param second
     *         an event message or {@code Event} to be imported second
     * @param rest
     *         optional event messages or instances of {@code Event} to be imported
     *         in the supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public final BlackBox
    importsEvents(EventMessage first, EventMessage second, EventMessage... rest) {
        return importAll(asList(first, second, rest));
    }

    private BlackBox importAll(Collection<EventMessage> eventMessages) {
        setup().importEvents(eventMessages);
        return this;
    }

    private BlackBoxSetup setup() {
        return new BlackBoxSetup(context, requestFactory(), memoizingObserver());
    }

    /**
     * Closes the {@code BlackBox} performing all necessary clean-ups.
     *
     * <p>This method performs the following:
     * <ol>
     *     <li>Closes tested {@link BoundedContext}.</li>
     *     <li>Closes associated {@link ClientSupplier}.</li>
     * </ol>
     *
     * <p>Instead of a checked {@link java.io.IOException IOException}, wraps any issues
     * that may occur while closing, into an {@link IllegalStateException}.
     */
    @Override
    public final void close() {
        if(!isOpen()) {
            return;
        }

        try {
            context.close();
            clientSupplier.close();
        } catch (Exception e) {
            throw illegalStateWithCauseOf(e);
        }
    }

    @Override
    public boolean isOpen() {
        return context().isOpen();
    }

    /**
     * Obtains the request factory to operate with.
     */
    @SuppressWarnings("TestOnlyProblems")   /* `TestActorRequestFactory` is not test-only. */
    abstract TestActorRequestFactory requestFactory();

    /**
     * Obtains immutable list of commands generated in this Bounded Context in response to posted
     * messages.
     *
     * <p>The returned list does <em>NOT</em> contain commands posted to this Bounded Context
     * during test setup.
     */
    private ImmutableList<Command> commands() {
        Predicate<Command> wasNotReceived =
                ((Predicate<Command>) postedCommands::contains).negate();
        return select(this.commands)
                .stream()
                .filter(wasNotReceived)
                .collect(toImmutableList());
    }

    /**
     * Selects commands that belong to the current tenant.
     */
    abstract ImmutableList<Command> select(CommandCollector collector);

    /**
     * Obtains immutable list of events generated in this Bounded Context in response to posted
     * messages.
     *
     * <p>The returned list does <em>NOT</em> contain events posted to this Bounded Context
     * during test setup.
     */
    private ImmutableList<Event> events() {
        Predicate<Event> wasNotReceived = ((Predicate<Event>) postedEvents::contains).negate();
        return allEvents()
                .stream()
                .filter(wasNotReceived)
                .collect(toImmutableList());
    }

    /**
     * Selects events that belong to the current tenant.
     */
    abstract ImmutableList<Event> select(EventCollector collector);

    /**
     * Obtains immutable list of all the events in this Bounded Context.
     */
    @VisibleForTesting
    final ImmutableList<Event> allEvents() {
        return select(this.events);
    }

    /**
     * Performs data reading operation in a tenant context.
     */
    protected <@Nullable D> D readOperation(Supplier<D> supplier) {
        return supplier.get();
    }

    /**
     * Obtains a Subject for an entity of the passed class with the given ID.
     */
    public final <I, E extends Entity<I, ? extends EntityState<I>>>
    EntitySubject assertEntity(I id, Class<E> entityClass) {
        @Nullable Entity<I, ?> found = findEntity(id, entityClass);
        return EntitySubject.assertEntity(found);
    }

    private <I> @Nullable Entity<I, ?>
    findEntity(I id, Class<? extends Entity<I, ?>> entityClass) {
        Class<? extends EntityState<I>> stateClass = stateClassOf(entityClass);
        return findByState(id, stateClass);
    }

    /**
     * Obtains a Subject for an entity which has the state of the passed class with the given ID.
     */
    public final <I, S extends EntityState<I>>
    EntitySubject assertEntityWithState(I id, Class<S> stateClass) {
        @Nullable Entity<I, S> found = findByState(id, stateClass);
        return EntitySubject.assertEntity(found);
    }

    private <I, S extends EntityState<I>> @Nullable Entity<I, S>
    findByState(I id, Class<S> stateClass) {
        @SuppressWarnings("unchecked")
        Repository<I, ? extends Entity<I, S>> repo =
                (Repository<I, ? extends Entity<I, S>>) repositoryOf(stateClass);
        return readOperation(() -> (Entity<I, S>) repo.find(id)
                                                      .orElse(null));
    }

    @VisibleForTesting
    final Repository<?, ?> repositoryOf(Class<? extends EntityState<?>> stateClass) {
        Repository<?, ?> repository =
                context.internalAccess()
                       .getRepository(stateClass);
        return repository;
    }

    /**
     * Asserts that the is an entity with the passed ID and the passed type of state.
     *
     * @return assertion that compares only expected fields
     */
    public final <I, S extends EntityState<I>> ProtoFluentAssertion
    assertState(I id, Class<S> stateClass) {
        checkNotNull(id);
        checkNotNull(stateClass);
        ProtoFluentAssertion stateAssertion =
                assertEntityWithState(id, stateClass)
                        .hasStateThat()
                        .comparingExpectedFieldsOnly();
        return stateAssertion;
    }

    /**
     * Asserts that there is an entity with the passed ID and the passed state.
     *
     * <p>The method compares only fields in the passed state.
     */
    public final <I, S extends EntityState<I>> void assertState(I id, S entityState) {
        checkNotNull(id);
        checkNotNull(entityState);
        @SuppressWarnings("unchecked")  // Guaranteed by `S` boundaries.
        Class<? extends EntityState<I>> typedWithI =
                (Class<? extends EntityState<I>>) entityState.getClass();
        assertState(id, typedWithI)
                .isEqualTo(entityState);
    }

    /**
     * Obtains the subject for checking commands generated by the entities of this Bounded Context.
     */
    public final CommandSubject assertCommands() {
        return CommandSubject.assertThat(commands());
    }

    /**
     * Obtains the subject for checking events emitted by the entities of this Bounded Context.
     */
    public final EventSubject assertEvents() {
        return EventSubject.assertThat(events());
    }

    /**
     * Asserts that the context emitted only one event of the passed type.
     *
     * @param eventClass
     *         the type of the event to assert
     * @return the subject for further assertions
     */
    @CanIgnoreReturnValue
    public final ProtoFluentAssertion assertEvent(Class<? extends EventMessage> eventClass) {
        EventSubject assertEvents =
                assertEvents().withType(eventClass);
        assertEvents.hasSize(1);
        return assertEvents.message(0)
                           .comparingExpectedFieldsOnly();
    }

    /**
     * Asserts that the context emitted only one passed event.
     */
    public final void assertEvent(EventMessage event) {
        assertEvent(event.getClass()).isEqualTo(event);
    }

    /**
     * Obtains the subject for checking the {@code Query} execution result.
     */
    @SuppressWarnings("TestOnlyProblems")   /* `QueryResultSubject` is not test-only. */
    public QueryResultSubject assertQueryResult(Query query) {
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        QueryService service = QueryService
                .newBuilder()
                .add(context)
                .build();
        service.read(query, observer);
        assertTrue(observer.isCompleted());

        QueryResponse response = observer.firstResponse();
        return QueryResultSubject.assertQueryResult(response);
    }

    /**
     * Subscribes and activates the subscription to the passed topic.
     *
     * @param topic
     *         the topic of the subscription
     * @return a fixture for testing subscription updates.
     */
    public SubscriptionFixture subscribeTo(Topic topic) {
        SubscriptionFixture result = new SubscriptionFixture(context, topic);
        result.activate();
        return result;
    }

    /**
     * Creates a new instance of {@link Client} linked to this context.
     *
     * <p>Provided {@code Client} would inherit {@code TenantId} from {@code BlackBoxContext},
     * but would NOT inherit {@code UserId} and {@code ZoneId}.
     *
     * @see #withTenant(TenantId)
     * @see #withActor(UserId)
     * @see #in(ZoneId)
     */
    public Client client() {
        @Nullable TenantId tenantId = requestFactory().tenantId();
        Client result = clientSupplier.create(tenantId);
        return result;
    }
}
