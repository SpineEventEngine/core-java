/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
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
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.QueryService;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.event.EventStore;
import io.spine.server.integration.IntegrationBroker;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.CommandSubject;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.entity.EntitySubject;
import io.spine.testing.server.query.QueryResultSubject;
import io.spine.time.ZoneId;
import io.spine.time.ZoneOffset;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.asList;
import static com.google.common.collect.Maps.newHashMap;
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
 *
 * @apiNote It is expected that instances of classes derived from
 *         {@code BlackBoxBoundedContext} are obtained by factory
 *         methods provided by this class.
 */
@SuppressWarnings({
        "ClassWithTooManyMethods",
        "OverlyCoupledClass"})
@VisibleForTesting
public abstract class BlackBoxContext
        implements Logging {

    /** The context under the test. */
    private final BoundedContext context;

    /**
     * All the repositories of the context under the text mapped by
     * the type of the state of their entities.
     */
    private final Map<Class<? extends EntityState>, Repository<?, ?>> repositories;

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
     * Information about the current user and the time-zone.
     */
    private Actor actor;

    /**
     * Creates new instance obtaining configuration parameters from the passed builder.
     */
    public static BlackBoxContext from(BoundedContextBuilder builder) {
        BlackBoxContext result = create(builder);
        builder.repositories()
               .forEach(result::registerRepository);
        builder.commandDispatchers()
               .forEach(result::registerCommandDispatcher);
        builder.eventDispatchers()
               .forEach(result::registerEventDispatcher);

        return result;
    }

    private static BlackBoxContext create(BoundedContextBuilder builder) {
        EventEnricher enricher =
                builder.eventEnricher()
                       .orElseGet(BlackBoxContext::emptyEnricher);
        String name = builder.name()
                             .value();
        return builder.isMultitenant()
               ? new MultiTenantContext(name, enricher)
               : new SingleTenantContext(name, enricher);
    }

    protected BlackBoxContext(String name, boolean multitenant, EventEnricher enricher) {
        super();
        this.commands = new CommandCollector();
        this.postedCommands = synchronizedSet(new HashSet<>());
        this.events = new EventCollector();
        this.postedEvents = synchronizedSet(new HashSet<>());
        BoundedContextBuilder builder =
                multitenant
                ? BoundedContext.multitenant(name)
                : BoundedContext.singleTenant(name);
        this.context = builder
                .addCommandListener(commands)
                .addEventListener(events)
                .enrichEventsUsing(enricher)
                .addEventDispatcher(new UnsupportedCommandGuard(name))
                .addEventDispatcher(DiagnosticLog.instance())
                .build();
        this.repositories = newHashMap();
        this.actor = defaultActor();
    }

    /** Obtains the name of this bounded context. */
    public BoundedContextName name() {
        return context.name();
    }

    /**
     * Makes all future requests to the context come to the tenant with the passed ID.
     *
     * @throws IllegalStateException
     *         if the method is called for a single-tenant context
     */
    public abstract BlackBoxContext withTenant(TenantId tenant);

    /**
     * Sets the given {@link UserId} as the actor ID for the requests produced by this context.
     */
    public final BlackBoxContext withActor(UserId user) {
        this.actor = Actor.from(user);
        return this;
    }

    /**
     * Sets the given time zone parameters for the actor requests produced by this context.
     */
    public final BlackBoxContext in(ZoneId zoneId, ZoneOffset zoneOffset) {
        this.actor = Actor.from(zoneId, zoneOffset);
        return this;
    }

    /**
     * Sets the given actor ID and time zone parameters for the actor requests produced by this
     * context.
     */
    public final BlackBoxContext withActorIn(UserId userId, ZoneId zoneId, ZoneOffset zoneOffset) {
        this.actor = Actor.from(userId, zoneId, zoneOffset);
        return this;
    }

    /**
     * Obtains the current {@link Actor}.
     */
    protected final Actor actor() {
        return this.actor;
    }

    /**
     * Obtains repositories registered with the context.
     */
    @VisibleForTesting
    Collection<Repository<?, ?>> repositories() {
        return repositories.values();
    }

    /**
     * Obtains set of type names of entities known to this Bounded Context.
     */
    @VisibleForTesting
    Set<TypeName> allStateTypes() {
        return context.stateTypes();
    }

    /** Obtains {@code event bus} instance used by this bounded context. */
    @VisibleForTesting
    EventBus eventBus() {
        return context.eventBus();
    }

    /**
     * Appends the passed event to the history of the context under the test.
     */
    @CanIgnoreReturnValue
    public BlackBoxContext append(Event event) {
        checkNotNull(event);
        EventStore eventStore =
                context.eventBus()
                       .eventStore();
        eventStore.append(event);
        return this;
    }

    /** Obtains {@code command bus} instance used by this bounded context. */
    @VisibleForTesting
    CommandBus commandBus() {
        return context.commandBus();
    }

    private void registerRepository(Repository<?, ?> repository) {
        context.register(repository);
        remember(repository);
    }

    private void remember(Repository<?, ?> repository) {
        Class<? extends EntityState> stateClass =
                repository.entityModelClass()
                          .stateClass();
        repositories.put(stateClass, repository);
    }

    private void registerCommandDispatcher(CommandDispatcher dispatcher) {
        if (dispatcher instanceof Repository) {
            registerRepository((Repository<?, ?>) dispatcher);
        } else {
            context.registerCommandDispatcher(dispatcher);
        }
    }

    /**
     * Registers the specified event dispatcher with the context under the test.
     */
    private void registerEventDispatcher(EventDispatcher dispatcher) {
        if (dispatcher instanceof Repository) {
            registerRepository((Repository<?, ?>) dispatcher);
        } else {
            context.registerEventDispatcher(dispatcher);
        }
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
    public BlackBoxContext receivesCommand(CommandMessage domainCommand) {
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
    public BlackBoxContext
    receivesCommands(CommandMessage first, CommandMessage second, CommandMessage... rest) {
        return receivesCommands(asList(first, second, rest));
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommands
     *         a list of domain commands to be dispatched to the Bounded Context
     * @return current instance
     */
    private BlackBoxContext receivesCommands(Collection<CommandMessage> domainCommands) {
        List<Command> posted = setup().postCommands(domainCommands);
        postedCommands.addAll(posted);
        return this;
    }

    /**
     * Sends off a provided event to the Bounded Context.
     *
     * @param messageOrEvent
     *         an event message or {@link io.spine.core.Event}. If an instance of {@code Event} is
     *         passed, it will be posted to {@link EventBus} as is.
     *         Otherwise, an instance of {@code Event} will be generated basing on the passed
     *         event message and posted to the bus.
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public BlackBoxContext receivesEvent(EventMessage messageOrEvent) {
        return receivesEvents(singletonList(messageOrEvent));
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
    @CanIgnoreReturnValue
    public BlackBoxContext
    receivesEvents(EventMessage first, EventMessage second, EventMessage... rest) {
        return receivesEvents(asList(first, second, rest));
    }

    /**
     * Sends off a provided event to the Bounded Context as event from an external source.
     *
     * @param messageOrEvent
     *         an event message or {@link Event}. If an instance of {@code Event} is
     *         passed, it will be posted to {@link IntegrationBroker} as is. Otherwise, an instance
     *         of {@code Event} will be generated basing on the passed event message and posted to
     *         the bus.
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public BlackBoxContext receivesExternalEvent(Message messageOrEvent) {
        setup().postExternalEvent(messageOrEvent);
        return this;
    }

    /**
     * Sends off provided events to the Bounded Context as events from an external source.
     *
     * <p>The method accepts event messages or instances of {@link io.spine.core.Event}.
     * If an instance of {@code Event} is passed, it will be posted to
     * {@link IntegrationBroker} as is.
     * Otherwise, an instance of {@code Event} will be generated basing on the passed event
     * message and posted to the bus.
     *
     * @param firstEvent
     *         an external event to be dispatched first
     * @param secondEvent
     *         an external event to be dispatched second
     * @param otherEvents
     *         optional external events to be dispatched in the supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public BlackBoxContext
    receivesExternalEvents(EventMessage firstEvent,
                           EventMessage secondEvent,
                           EventMessage... otherEvents) {
        return receivesExternalEvents(asList(firstEvent, secondEvent, otherEvents));
    }

    /**
     * Sends off provided events to the Bounded Context as events from an external source.
     *
     * @param eventMessages
     *         a list of external events to be dispatched to the Bounded Context
     * @return current instance
     */
    private BlackBoxContext receivesExternalEvents(Collection<EventMessage> eventMessages) {
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
    public BlackBoxContext
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
    private BlackBoxContext receivesEvents(Collection<EventMessage> domainEvents) {
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
    public BlackBoxContext importsEvent(Message eventOrMessage) {
        setup().importEvent(eventOrMessage);
        return this;
    }

    /**
     * Imports passed events into the Bounded Context.
     *
     * @param first
     *         an event message or {@code Event} to be imported first
     * @param second
     *         an event message or {@code Event} to be imported first second
     * @param rest
     *         optional event messages or instances of {@code Event} to be imported
     *         in the supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public BlackBoxContext
    importsEvents(EventMessage first, EventMessage second, EventMessage... rest) {
        return importAll(asList(first, second, rest));
    }

    private BlackBoxContext importAll(Collection<EventMessage> eventMessages) {
        setup().importEvents(eventMessages);
        return this;
    }

    private BlackBoxSetup setup() {
        return new BlackBoxSetup(context, requestFactory(), memoizingObserver());
    }

    /**
     * Closes the bounded context so that it shutting down all of its repositories.
     *
     * <p>Instead of a checked {@link java.io.IOException IOException}, wraps any issues
     * that may occur while closing, into an {@link IllegalStateException}.
     */
    public void close() {
        try {
            context.close();
        } catch (Exception e) {
            throw illegalStateWithCauseOf(e);
        }
    }

    /**
     * Obtains the request factory to operate with.
     */
    abstract TestActorRequestFactory requestFactory();

    /**
     * Obtains immutable list of commands generated in this Bounded Context in response to posted
     * messages.
     *
     * <p>The returned list does <em>NOT</em> contain commands posted to this Bounded Context
     * during test setup.
     *
     * @see #commandMessages()
     */
    public ImmutableList<Command> commands() {
        Predicate<Command> wasNotReceived =
                ((Predicate<Command>) postedCommands::contains).negate();
        return select(this.commands)
                .stream()
                .filter(wasNotReceived)
                .collect(toImmutableList());
    }

    /**
     * Obtains immutable list of command messages generated in this Bounded Context in response
     * to posted messages.
     *
     * <p>The returned list does <em>NOT</em> contain commands posted to this Bounded Context
     * during test setup.
     *
     * @see #commands()
     */
    public ImmutableList<CommandMessage> commandMessages() {
        return commands().stream()
                         .map(Command::getMessage)
                         .map(AnyPacker::unpack)
                         .map(m -> (CommandMessage) m)
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
    public ImmutableList<Event> events() {
        Predicate<Event> wasNotReceived = ((Predicate<Event>) postedEvents::contains).negate();
        return select(this.events)
                .stream()
                .filter(wasNotReceived)
                .collect(toImmutableList());
    }

    /**
     * Obtains immutable list of all the events in this Bounded Context.
     */
    @VisibleForTesting
    ImmutableList<Event> allEvents() {
        return select(this.events);
    }

    /**
     * Obtains immutable list of event messages generated in this Bounded Context in response
     * to posted messages.
     *
     * <p>The returned list does <em>NOT</em> contain events posted to this Bounded Context
     * during test setup.
     *
     * @see #events()
     */
    public List<EventMessage> eventMessages() {
        return events().stream()
                       .map(Event::enclosedMessage)
                       .collect(toImmutableList());
    }

    /**
     * Selects events that belong to the current tenant.
     */
    abstract ImmutableList<Event> select(EventCollector collector);

    private static EventEnricher emptyEnricher() {
        return EventEnricher.newBuilder()
                            .build();
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
    public <I, E extends Entity<I, ? extends EntityState>>
    EntitySubject assertEntity(Class<E> entityClass, I id) {
        @Nullable Entity<I, ?> found = findEntity(entityClass, id);
        return EntitySubject.assertEntity(found);
    }

    private <I> @Nullable Entity<I, ?>
    findEntity(Class<? extends Entity<I, ?>> entityClass, I id) {
        Class<? extends EntityState> stateClass = stateClassOf(entityClass);
        return findByState(stateClass, id);
    }

    /**
     * Obtains a Subject for an entity which has the state of the passed class with the given ID.
     */
    public <I, S extends EntityState> EntitySubject
    assertEntityWithState(Class<S> stateClass, I id) {
        @Nullable Entity<I, S> found = findByState(stateClass, id);
        return EntitySubject.assertEntity(found);
    }

    private <I, S extends EntityState> @Nullable Entity<I, S>
    findByState(Class<S> stateClass, I id) {
        @SuppressWarnings("unchecked")
        Repository<I, ? extends Entity<I, S>> repo =
                (Repository<I, ? extends Entity<I, S>>) repositoryOf(stateClass);
        return readOperation(() -> (Entity<I, S>) repo.find(id).orElse(null));
    }

    @VisibleForTesting
    private Repository<?, ?> repositoryOf(Class<? extends EntityState> stateClass) {
        Repository<?, ?> repository = repositories.get(stateClass);
        return repository;
    }

    /**
     * Obtains the subject for checking commands generated by the entities of this Bounded Context.
     */
    public CommandSubject assertCommands() {
        return CommandSubject.assertThat(commands());
    }

    /**
     * Obtains the subject for checking events emitted by the entities of this Bounded Context.
     */
    public EventSubject assertEvents() {
        return EventSubject.assertThat(events());
    }

    /**
     * Obtains the subject for checking the {@code Query} execution result.
     */
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
     * @param topic
     *          the topic of the subscription
     * @return a fixture for testing subscription updates.
     */
    public SubscriptionFixture subscribeTo(Topic topic) {
        SubscriptionFixture result = new SubscriptionFixture(context, topic);
        result.activate();
        return result;
    }
}
