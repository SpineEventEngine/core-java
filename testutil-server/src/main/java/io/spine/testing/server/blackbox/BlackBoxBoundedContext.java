/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.logging.Logging;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.QueryService;
import io.spine.server.SubscriptionService;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.integration.IntegrationBroker;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.CommandErrored;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.CommandSubject;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.SubscriptionActivator;
import io.spine.testing.server.SubscriptionObserver;
import io.spine.testing.server.VerifyingCounter;
import io.spine.testing.server.blackbox.verify.query.QueryResultSubject;
import io.spine.testing.server.blackbox.verify.state.VerifyState;
import io.spine.testing.server.blackbox.verify.subscription.ToProtoSubjects;
import io.spine.testing.server.entity.EntitySubject;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.asList;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.core.BoundedContextNames.assumingTestsValue;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.entity.model.EntityClass.stateClassOf;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class provides means for integration testing of Bounded Contexts.
 *
 * <p>Such a test suite would send commands or events to the Bounded Context under the test,
 * and then verify consequences of handling a command or an event.
 *
 * <p>Handling a command or an event usually results in {@link VerifyEvents emitted events}) and
 * {@linkplain VerifyState updated state} of an entity. This class provides API for testing such
 * effects.
 *
 * @param <T>
 *         the type of a sub-class for return type covariance
 * @apiNote It is expected that instances of classes derived from
 *         {@code BlackBoxBoundedContext} are obtained by factory
 *         methods provided by this class.
 */
@SuppressWarnings({
        "ClassReferencesSubclass", /* See the API note. */
        "ClassWithTooManyMethods",
        "OverlyCoupledClass"})
@VisibleForTesting
public abstract class BlackBoxBoundedContext<T extends BlackBoxBoundedContext>
        extends AbstractEventSubscriber
        implements Logging {

    private final BoundedContext context;

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
     *
     * @see #emittedEvents()
     */
    private final Set<Event> postedEvents;

    private final MemoizingObserver<Ack> observer;

    /**
     * A guard that verifies that unsupported commands do not get posted to the
     * {@code BlackBoxBoundedContext}.
     */
    private final UnsupportedCommandGuard unsupportedCommandGuard;

    private final Map<Class<? extends Message>, Repository<?, ?>> repositories;

    @SuppressWarnings("ThisEscapedInObjectConstruction") // to inject self as event dispatcher.
    protected BlackBoxBoundedContext(boolean multitenant,
                                     EventEnricher enricher,
                                     String name) {
        super();
        this.commands = new CommandCollector();
        this.postedCommands = new HashSet<>();
        this.events = new EventCollector();
        this.postedEvents = new HashSet<>();
        BoundedContextBuilder builder =
                multitenant
                ? BoundedContext.multitenant(name)
                : BoundedContext.singleTenant(name);
        this.context = builder
                .addCommandListener(commands)
                .addEventListener(events)
                .enrichEventsUsing(enricher)
                .build();
        this.observer = memoizingObserver();
        this.unsupportedCommandGuard = new UnsupportedCommandGuard(name);
        this.repositories = newHashMap();
        this.context.registerEventDispatcher(this);
    }

    /**
     * Creates a single-tenant instance with the default configuration.
     */
    public static SingleTenantBlackBoxContext singleTenant() {
        return singleTenant(emptyEnricher());
    }

    /**
     * Creates a single-tenant instance with the specified name.
     */
    public static SingleTenantBlackBoxContext singleTenant(String name) {
        return singleTenant(name, emptyEnricher());
    }

    /**
     * Creates a single-tenant instance with the specified enricher.
     */
    public static SingleTenantBlackBoxContext singleTenant(EventEnricher enricher) {
        return singleTenant(assumingTestsValue(), enricher);
    }

    /**
     * Creates a single-tenant instance with the specified name and enricher.
     */
    public static SingleTenantBlackBoxContext singleTenant(String name, EventEnricher enricher) {
        return new SingleTenantBlackBoxContext(name, enricher);
    }

    /**
     * Creates a multitenant instance the default configuration.
     */
    public static MultitenantBlackBoxContext multiTenant() {
        return multiTenant(emptyEnricher());
    }

    /**
     * Creates a multitenant instance with the specified name.
     */
    public static MultitenantBlackBoxContext multiTenant(String name) {
        return multiTenant(name, emptyEnricher());
    }

    /**
     * Creates a multitenant instance with the specified enricher.
     */
    public static MultitenantBlackBoxContext multiTenant(EventEnricher enricher) {
        return multiTenant(assumingTestsValue(), enricher);
    }

    /**
     * Creates a multitenant instance with the specified name and enricher.
     */
    public static MultitenantBlackBoxContext multiTenant(String name, EventEnricher enricher) {
        return new MultitenantBlackBoxContext(name, enricher);
    }

    /**
     * Creates new instance obtaining configuration parameters from the passed builder.
     *
     * <p>In particular:
     * <ul>
     *     <li>multi-tenancy status;
     *     <li>{@code Enricher};
     *     <li>added repositories.
     * </ul>
     */
    public static BlackBoxBoundedContext<?> from(BoundedContextBuilder builder) {
        EventEnricher enricher =
                builder.eventEnricher()
                       .orElseGet(BlackBoxBoundedContext::emptyEnricher);
        BlackBoxBoundedContext<?> result =
                builder.isMultitenant()
                ? multiTenant(enricher)
                : singleTenant(enricher);

        builder.repositories()
               .forEach(result::with);
        builder.commandDispatchers()
               .forEach(result::withHandlers);
        builder.eventDispatchers()
               .forEach(result::withEventDispatchers);

        return result;
    }

    /**
     * Throws an {@link AssertionError}.
     *
     * <p>Only reachable after {@code unsupportedGuard} has
     * {@linkplain #canDispatch(EventEnvelope) detected} a violation.
     */
    @Override
    protected void handle(EventEnvelope event) {
        unsupportedCommandGuard.failTest();
    }

    /**
     * Checks if the given {@link CommandErrored} message represents an unsupported command
     * {@linkplain io.spine.server.commandbus.UnsupportedCommandException error}.
     */
    @Override
    public boolean canDispatch(EventEnvelope eventEnvelope) {
        CommandErrored event = (CommandErrored) eventEnvelope.message();
        return unsupportedCommandGuard.checkAndRemember(event);
    }

    @Override
    public Set<EventClass> messageClasses() {
        Set<EventClass> result = singleton(EventClass.from(CommandErrored.class));
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code BlackBoxBoundedContext} does not consume external events.
     */
    @Override
    public Set<EventClass> externalEventClasses() {
        return ImmutableSet.of();
    }

    /** Obtains the name of this bounded context. */
    public BoundedContextName name() {
        return context.name();
    }

    /**
     * Obtains set of type names of entities known to this Bounded Context.
     */
    @VisibleForTesting
    Set<TypeName> allStateTypes() {
        return context.stateTypes();
    }

    /** Obtains {@code event bus} instance used by this bounded context. */
    public EventBus eventBus() {
        return context.eventBus();
    }

    /** Obtains {@code command bus} instance used by this bounded context. */
    public CommandBus commandBus() {
        return context.commandBus();
    }

    /**
     * Registers passed repositories with the Bounded Context under the test.
     *
     * @param repositories
     *         repositories to register in the Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public final T with(Repository<?, ?>... repositories) {
        registerAll(this::registerRepository, repositories);
        return thisRef();
    }

    private void registerRepository(Repository<?, ?> repository) {
        context.register(repository);
        remember(repository);
    }

    private void remember(Repository<?, ?> repository) {
        Class<Message> stateClass = repository.entityStateType()
                                              .getMessageClass();
        repositories.put(stateClass, repository);
    }

    /**
     * Registers the specified command dispatchers with this bounded context.
     *
     * @param dispatchers
     *         command dispatchers to register with the bounded context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public final T withHandlers(CommandDispatcher... dispatchers) {
        registerAll(this::registerCommandDispatcher, dispatchers);
        return thisRef();
    }

    private void registerCommandDispatcher(CommandDispatcher dispatcher) {
        if (dispatcher instanceof Repository) {
            registerRepository((Repository<?, ?>) dispatcher);
        } else {
            context.registerCommandDispatcher(dispatcher);
        }
    }

    /**
     * Registers the specified event dispatchers with the {@code event bus} of this
     * bounded context.
     *
     * @param dispatchers
     *         dispatchers to register with the event bus of this bounded context
     */
    @CanIgnoreReturnValue
    public final T withEventDispatchers(EventDispatcher... dispatchers) {
        registerAll(this::registerEventDispatcher, dispatchers);
        return thisRef();
    }

    private void registerEventDispatcher(EventDispatcher dispatcher) {
        if (dispatcher instanceof Repository) {
            registerRepository((Repository<?, ?>) dispatcher);
        } else {
            context.registerEventDispatcher(dispatcher);
        }
    }

    @SafeVarargs
    private static <S> void registerAll(Consumer<S> registerFn, S... itemsToRegister) {
        checkNotNull(itemsToRegister);
        for (S item : itemsToRegister) {
            checkNotNull(item);
            registerFn.accept(item);
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
    public T receivesCommand(CommandMessage domainCommand) {
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
    public T receivesCommands(CommandMessage first, CommandMessage second, CommandMessage... rest) {
        return receivesCommands(asList(first, second, rest));
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommands
     *         a list of domain commands to be dispatched to the Bounded Context
     * @return current instance
     */
    private T receivesCommands(Collection<CommandMessage> domainCommands) {
        List<Command> posted = setup().postCommands(domainCommands);
        postedCommands.addAll(posted);
        return thisRef();
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
    public T receivesEvent(EventMessage messageOrEvent) {
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
     *         a domain event to be dispatched to the Bounded Context first
     * @param second
     *         a domain event to be dispatched to the Bounded Context second
     * @param rest
     *         optional domain events to be dispatched to the Bounded Context in supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public T receivesEvents(EventMessage first, EventMessage second, EventMessage... rest) {
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
    public T receivesExternalEvent(Message messageOrEvent) {
        setup().postExternalEvent(messageOrEvent);
        return thisRef();
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
     *         an external event to be dispatched to the Bounded Context first
     * @param secondEvent
     *         an external event to be dispatched to the Bounded Context second
     * @param otherEvents
     *         optional external events to be dispatched to the Bounded Context
     *         in supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public T receivesExternalEvents(EventMessage firstEvent,
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
    private T receivesExternalEvents(Collection<EventMessage> eventMessages) {
        setup().postExternalEvents(eventMessages);
        return thisRef();
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
     *         optional domain events to be dispatched to the Bounded Context in supplied order
     * @return current instance
     */
    @CanIgnoreReturnValue
    public T receivesEventsProducedBy(Object producerId, EventMessage first, EventMessage... rest) {
        List<Event> sentEvents = setup().postEvents(producerId, first, rest);
        postedEvents.addAll(sentEvents);
        return thisRef();
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param domainEvents
     *         a list of domain event to be dispatched to the Bounded Context
     * @return current instance
     */
    private T receivesEvents(Collection<EventMessage> domainEvents) {
        List<Event> sentEvents = setup().postEvents(domainEvents);
        this.postedEvents.addAll(sentEvents);
        return thisRef();
    }

    @CanIgnoreReturnValue
    public T importsEvent(Message eventOrMessage) {
        setup().importEvent(eventOrMessage);
        return thisRef();
    }

    @CanIgnoreReturnValue
    public T importsEvents(EventMessage first, EventMessage second, EventMessage... rest) {
        return importAll(asList(first, second, rest));
    }

    private T importAll(Collection<EventMessage> eventMessages) {
        setup().importEvents(eventMessages);
        return thisRef();
    }

    /**
     * Asserts that an event of the passed class was emitted once.
     *
     * @param eventClass
     *         the class of events to verify
     * @return current instance
     * @deprecated use {@link #assertEvents()} instead; to be removed in future versions
     */
    @Deprecated
    @CanIgnoreReturnValue
    public T assertEmitted(Class<? extends EventMessage> eventClass) {
        assertEvents()
                .withType(eventClass)
                .hasSize(1);
        return thisRef();
    }

    /**
     * Asserts that a rejection of the passed class was emitted once.
     *
     * @param rejectionClass
     *         the class of the rejection to verify
     * @return current instance
     * @deprecated use {@link #assertEvents()} instead; to be removed in future versions
     */
    @Deprecated
    @CanIgnoreReturnValue
    public T assertRejectedWith(Class<? extends RejectionMessage> rejectionClass) {
        return assertEmitted(rejectionClass);
    }

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     * @deprecated use {@link #assertEvents()} instead; to be removed in future versions
     */
    @Deprecated
    @CanIgnoreReturnValue
    public T assertThat(VerifyEvents verifier) {
        EmittedEvents events = emittedEvents();
        verifier.verify(events);
        return thisRef();
    }

    /**
     * Executes the provided verifier, which throws an assertion error in case of
     * unexpected results.
     *
     * @param verifier
     *         a verifier that checks the acknowledgements in this Bounded Context
     * @return current instance
     * @deprecated verify command outcome instead of acknowledgements; to be removed in future
     *         versions
     */
    @Deprecated
    @CanIgnoreReturnValue
    public T assertThat(VerifyAcknowledgements verifier) {
        Acknowledgements acks = commandAcknowledgements(observer);
        verifier.verify(acks);
        return thisRef();
    }

    /**
     * Verifies emitted commands by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the commands emitted in this Bounded Context
     * @return current instance
     * @deprecated use {@link #assertCommands()} instead; to be removed in future versions
     */
    @Deprecated
    @CanIgnoreReturnValue
    public T assertThat(VerifyCommands verifier) {
        EmittedCommands commands = emittedCommands();
        verifier.verify(commands);
        return thisRef();
    }

    /**
     * Asserts the state of an entity using the specified tenant ID.
     *
     * @param verifier
     *         a verifier of entity states
     * @return current instance
     * @deprecated use {@link #assertEntity}, {@link #assertEntityWithState},
     *         or {@link #assertQueryResult} instead; to be removed in future versions
     */
    @Deprecated
    @CanIgnoreReturnValue
    public T assertThat(VerifyState verifier) {
        QueryFactory queryFactory = requestFactory().query();
        verifier.verify(context, queryFactory);
        return thisRef();
    }

    private BlackBoxSetup setup() {
        return new BlackBoxSetup(context, requestFactory(), observer);
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

    /** Casts this to generic type to provide type covariance in the derived classes. */
    @SuppressWarnings("unchecked" /* See Javadoc. */)
    private T thisRef() {
        return (T) this;
    }

    /**
     * Obtains the request factory to operate with.
     */
    protected abstract TestActorRequestFactory requestFactory();

    /**
     * Obtains commands emitted in the bounded context.
     */
    private EmittedCommands emittedCommands() {
        List<Command> allWithoutPosted = commands();
        return new EmittedCommands(allWithoutPosted);
    }

    /**
     * Obtains immutable list of commands generated in this Bounded Context in response to posted
     * messages.
     *
     * <p>The returned list does <em>NOT</em> contain commands posted to this Bounded Context
     * during test setup.
     *
     * @see #commandMessages()
     */
    public List<Command> commands() {
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
    public List<CommandMessage> commandMessages() {
        return commands().stream()
                         .map(Command::getMessage)
                         .map(AnyPacker::unpack)
                         .map(m -> (CommandMessage) m)
                         .collect(toImmutableList());
    }

    /**
     * Selects commands that belong to the current tenant.
     */
    protected abstract List<Command> select(CommandCollector collector);

    /**
     * Obtains acknowledgements of {@linkplain #emittedCommands()
     * emitted commands}.
     */
    private static Acknowledgements commandAcknowledgements(MemoizingObserver<Ack> observer) {
        List<Ack> acknowledgements = observer.responses();
        return new Acknowledgements(acknowledgements);
    }

    /**
     * Obtains events emitted in the Bounded Context.
     *
     * <p>They do not include the events posted to the bounded context via {@code receivesEvent...}
     * calls.
     */
    private EmittedEvents emittedEvents() {
        List<Event> allWithoutPosted = events();
        return new EmittedEvents(allWithoutPosted);
    }

    /**
     * Obtains immutable list of events generated in this Bounded Context in response to posted
     * messages.
     *
     * <p>The returned list does <em>NOT</em> contain events posted to this Bounded Context
     * during test setup.
     */
    public List<Event> events() {
        Predicate<Event> wasNotReceived = ((Predicate<Event>) postedEvents::contains).negate();
        return select(this.events)
                .stream()
                .filter(wasNotReceived)
                .collect(toImmutableList());
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
    protected abstract List<Event> select(EventCollector collector);

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
    public <I, E extends Entity<I, ? extends Message>>
    EntitySubject assertEntity(Class<E> entityClass, I id) {
        @Nullable Entity<I, ?> found = findEntity(entityClass, id);
        return EntitySubject.assertEntity(found);
    }

    private <I> @Nullable Entity<I, ?> findEntity(Class<? extends Entity<I, ?>> entityClass, I id) {
        Class<? extends Message> stateClass = stateClassOf(entityClass);
        return findByState(stateClass, id);
    }

    /**
     * Obtains a Subject for an entity which has the state of the passed class with the given ID.
     */
    public <I, S extends Message> EntitySubject assertEntityWithState(Class<S> stateClass, I id) {
        @Nullable Entity<I, S> found = findByState(stateClass, id);
        return EntitySubject.assertEntity(found);
    }

    private <I, S extends Message> @Nullable Entity<I, S> findByState(Class<S> stateClass, I id) {
        @SuppressWarnings("unchecked")
        Repository<I, ? extends Entity<I, S>> repo =
                (Repository<I, ? extends Entity<I, S>>) repositoryOf(stateClass);
        return readOperation(() -> (Entity<I, S>) repo.find(id).orElse(null));
    }

    private Repository<?, ?> repositoryOf(Class<? extends Message> stateClass) {
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
        QueryService queryService = QueryService.newBuilder()
                                                .add(context)
                                                .build();
        queryService.read(query, observer);
        assertTrue(observer.isCompleted());

        QueryResponse response = observer.firstResponse();
        return QueryResultSubject.assertQueryResult(response);
    }

    /**
     * Subscribes to the {@code topic} and verifies the incoming updates.
     *
     * <p>The verification happens on a per-item basis, where item is a single entity state or
     * event update represented as {@link ProtoSubject}.
     *
     * <p>The returned value allows to check the number of updates received.
     *
     * <p>The method may be used as follows:
     * <pre>
     *     {@code
     *         VerifyingCounter updateCounter =
     *               context.assertSubscriptionUpdates(
     *                       topic,
     *                       assertEachReceived -> assertEachReceived.comparingExpectedFieldsOnly()
     *                                                               .isEqualTo(expected)
     *               );
     *         context.receivesCommand(createProject); // Some command creating the `expected`.
     *         updateCounter.verifyEquals(1);
     *     }
     * </pre>
     *
     * <p>Please note that the return value may be ignored, but then receiving {@code 0} incoming
     * updates will count as valid and won't fail the test.
     */
    @CanIgnoreReturnValue
    public VerifyingCounter
    assertSubscriptionUpdates(Topic topic, Consumer<ProtoSubject> assertEachReceived) {
        SubscriptionService subscriptionService =
                SubscriptionService.newBuilder()
                                   .add(context)
                                   .build();
        SubscriptionObserver updateObserver = new SubscriptionObserver(
                update -> new ToProtoSubjects().apply(update)
                                               .forEach(assertEachReceived)
        );
        StreamObserver<Subscription> activator =
                new SubscriptionActivator(subscriptionService, updateObserver);

        subscriptionService.subscribe(topic, activator);
        return updateObserver.counter();
    }
}