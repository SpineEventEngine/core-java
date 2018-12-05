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
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.client.QueryFactory;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.event.Enricher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStreamQuery;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.blackbox.verify.state.VerifyState;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.singletonList;

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
 * @param <T> the type of a sub-class for return type covariance
 * @apiNote It is expected that instances of classes derived from {@code BlackBoxBoundedContext}
 *          are obtained by factory methods provided by this class.
 */
@SuppressWarnings({
        "ClassReferencesSubclass", /* See the API note. */
        "ClassWithTooManyMethods",
        "OverlyCoupledClass"})
@VisibleForTesting
public abstract class BlackBoxBoundedContext<T extends BlackBoxBoundedContext> {

    private final BoundedContext boundedContext;
    private final CommandMemoizingTap commandTap;
    private final MemoizingObserver<Ack> observer;

    protected BlackBoxBoundedContext(boolean multitenant, Enricher enricher) {
        this.commandTap = new CommandMemoizingTap();
        EventBus.Builder eventBus = EventBus
                .newBuilder()
                .setEnricher(enricher);
        CommandBus.Builder commandBus = CommandBus
                .newBuilder()
                .appendFilter(commandTap);
        this.boundedContext = BoundedContext
                .newBuilder()
                .setMultitenant(multitenant)
                .setCommandBus(commandBus)
                .setEventBus(eventBus)
                .build();
        this.observer = memoizingObserver();
    }

    /**
     * Creates a single-tenant instance with the default configuration.
     */
    public static SingleTenantBlackBoxContext singleTenant() {
        return new SingleTenantBlackBoxContext(emptyEnricher());
    }

    /**
     * Creates new single-tenant instance with the default configuration.
     *
     * @deprecated use {@link #singleTenant()} or {@link #multiTenant()} instead
     */
    @Deprecated
    public static SingleTenantBlackBoxContext newInstance() {
        return singleTenant();
    }

    /**
     * Creates a single-tenant instance with the specified enricher.
     */
    public static SingleTenantBlackBoxContext singleTenant(Enricher enricher) {
        return new SingleTenantBlackBoxContext(enricher);
    }

    /**
     * Creates a multitenant instance the default configuration.
     */
    public static MultitenantBlackBoxContext multiTenant() {
        return new MultitenantBlackBoxContext(emptyEnricher());
    }

    /**
     * Creates a multitenant instance with the specified enricher.
     */
    public static MultitenantBlackBoxContext multiTenant(Enricher enricher) {
        return new MultitenantBlackBoxContext(enricher);
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
    public static BlackBoxBoundedContext from(BoundedContextBuilder builder) {
        Optional<EventBus.Builder> eventBus = builder.getEventBus();
        Enricher enricher =
                eventBus.isPresent()
                ? eventBus.get()
                          .getEnricher()
                          .orElse(emptyEnricher())
                : emptyEnricher();

        BlackBoxBoundedContext<?> result = builder.isMultitenant()
                ? multiTenant(enricher)
                : singleTenant(enricher);

        builder.repositories()
               .forEach(result::with);

        return result;
    }

    /**
     * Obtains set of type names of entities known to this Bounded Context.
     */
    @VisibleForTesting
    Set<TypeName> getAllEntityStateTypes() {
        ImmutableSet.Builder<TypeName> result = ImmutableSet.builder();
        for (Visibility visibility : Visibility.values()) {
            if (visibility == Visibility.VISIBILITY_UNKNOWN) {
                continue;
            }
            result.addAll(boundedContext.getEntityStateTypes(visibility));
        }
        return result.build();
    }

    @VisibleForTesting
    EventBus getEventBus() {
        return boundedContext.getEventBus();
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
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
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
     *         optional domain commands to be dispatched to the Bounded Context in supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public
    T receivesCommands(Message firstCommand, Message secondCommand, Message... otherCommands) {
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
        setup().postCommands(domainCommands);
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
     *         optional domain events to be dispatched to the Bounded Context in supplied order
     * @return current instance
     * @apiNote Returned value can be ignored when this method invoked for test setup.
     */
    @CanIgnoreReturnValue
    public T receivesEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
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
     *         optional domain events to be dispatched to the Bounded Context in supplied order
     * @return current instance
     */
    public T receivesEventsProducedBy(Object producerId,
                                      EventMessage firstEvent,
                                      EventMessage... otherEvents) {
        setup().postEvents(producerId, firstEvent, otherEvents);
        return thisRef();
    }

    /**
     * Sends off provided events to the Bounded Context.
     *
     * @param domainEvents
     *         a list of domain event to be dispatched to the Bounded Context
     * @return current instance
     */
    private T receivesEvents(Collection<Message> domainEvents) {
        setup().postEvents(domainEvents);
        return thisRef();
    }

    @CanIgnoreReturnValue
    public T importsEvent(Message eventOrMessage) {
        return this.importAll(singletonList(eventOrMessage));
    }

    @CanIgnoreReturnValue
    public T importsEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
        return this.importAll(asList(firstEvent, secondEvent, otherEvents));
    }

    private T importAll(Collection<Message> domainEvents) {
        setup().importEvents(domainEvents);
        return thisRef();
    }

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public T assertThat(VerifyEvents verifier) {
        EmittedEvents events = emittedEvents();
        verifier.verify(events);
        return thisRef();
    }

    /**
     * Asserts that an event of the passed class was emitted once.
     *
     * @param eventClass
     *         the class of events to verify
     * @return current instance
     */
    @CanIgnoreReturnValue
    public T assertEmitted(Class<? extends EventMessage> eventClass) {
        VerifyEvents verifier = VerifyEvents.emittedEvent(eventClass, once());
        EmittedEvents events = emittedEvents();
        verifier.verify(events);
        return thisRef();
    }

    /**
     * Asserts that a rejection of the passed class was emitted once.
     *
     * @param rejectionClass
     *         the class of the rejection to verify
     * @return current instance
     */
    @CanIgnoreReturnValue
    public T assertRejectedWith(Class<? extends RejectionMessage> rejectionClass) {
        VerifyEvents verifier = VerifyEvents.emittedEvent(rejectionClass, once());
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
     */
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
     */
    @CanIgnoreReturnValue
    public T assertThat(VerifyCommands verifier) {
        EmittedCommands commands = emittedCommands(commandTap);
        verifier.verify(commands);
        return thisRef();
    }

    /**
     * Asserts the state of an entity using the specified tenant ID.
     *
     * @param verifier
     *         a verifier of entity states
     * @return current instance
     */
    @CanIgnoreReturnValue
    public T assertThat(VerifyState verifier) {
        QueryFactory queryFactory = requestFactory().query();
        verifier.verify(boundedContext, queryFactory);
        return thisRef();
    }

    private BlackBoxSetup setup() {
        return new BlackBoxSetup(boundedContext, requestFactory(), observer);
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
    protected abstract EmittedCommands emittedCommands(CommandMemoizingTap commandTap);

    /**
     * Obtains acknowledgements of {@linkplain #emittedCommands(CommandMemoizingTap)
     * emitted commands}.
     */
    protected Acknowledgements commandAcknowledgements(MemoizingObserver<Ack> observer) {
        List<Ack> acknowledgements = observer.responses();
        return new Acknowledgements(acknowledgements);
    }

    /**
     * Obtains events emitted in the bounded context.
     */
    protected EmittedEvents emittedEvents() {
        MemoizingObserver<Event> queryObserver = memoizingObserver();
        boundedContext.getEventBus()
                      .getEventStore()
                      .read(allEventsQuery(), queryObserver);
        List<Event> responses = queryObserver.responses();
        return new EmittedEvents(responses);
    }

    /**
     * Creates a new {@link io.spine.server.event.EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    private static Enricher emptyEnricher() {
        return Enricher.newBuilder()
                       .build();
    }
}
