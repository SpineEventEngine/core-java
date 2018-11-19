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
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.server.QueryService;
import io.spine.server.event.EventBus;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.verify.state.VerifyState;
import io.spine.testing.server.blackbox.verify.state.VerifyState.VerifyStateByTenant;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.asList;
import static java.util.Collections.singletonList;

/**
 * A black box bounded context for writing integration tests in a multitenant environment.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
@VisibleForTesting
public class MultitenantBlackBoxContext
        extends BlackBoxBoundedContext<MultitenantBlackBoxContext> {

    private final TenantId tenantId;

    /**
     * Creates a new multi-tenant instance.
     */
    MultitenantBlackBoxContext(BlackBoxBuilder builder) {
        super(true,
              builder.buildEnricher(),
              requestFactory(builder.buildTenant()));
        this.tenantId = builder.buildTenant();
    }

    /**
     * Creates a new bounded context with the default configuration.
     */
    public static MultitenantBlackBoxContext newInstance() {
        return newBuilder().build();
    }

    /**
     * Creates a builder for a black box bounded context.
     */
    public static BlackBoxBuilder newBuilder() {
        return new BlackBoxBuilder();
    }

    /**
     * Creates a new {@link io.spine.client.ActorRequestFactory actor request factory} for tests
     * with a provided tenant ID.
     *
     * @param tenantId
     *         an identifier of a tenant that is executing requests in this Bounded Context
     * @return a new request factory instance
     */
    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(MultitenantBlackBoxContext.class, tenantId);
    }

    /**
     * Sends off a provided command to the Bounded Context.
     *
     * @param domainCommand
     *         a domain command to be dispatched to the Bounded Context
     * @return current instance
     */
    public MultitenantBlackBoxContext receivesCommand(Message domainCommand) {
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
    public MultitenantBlackBoxContext
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
    private MultitenantBlackBoxContext receivesCommands(Collection<Message> domainCommands) {
        input().receivesCommands(domainCommands);
        return this;
    }

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
    public MultitenantBlackBoxContext receivesEvent(Message messageOrEvent) {
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
    public MultitenantBlackBoxContext
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
    public MultitenantBlackBoxContext
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
    private MultitenantBlackBoxContext receivesEvents(Collection<Message> domainEvents) {
        input().receivesEvents(domainEvents);
        return this;
    }

    public MultitenantBlackBoxContext importsEvent(Message eventOrMessage) {
        return this.importAll(singletonList(eventOrMessage));
    }

    public MultitenantBlackBoxContext
    importsEvents(Message firstEvent, Message secondEvent, Message... otherEvents) {
        return this.importAll(asList(firstEvent, secondEvent, otherEvents));
    }

    private MultitenantBlackBoxContext importAll(Collection<Message> domainEvents) {
        input().importsEvents(domainEvents);
        return this;
    }

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyEvents verifier) {
        EmittedEvents events = emittedEvents();
        verifier.verify(events);
        return this;
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
    public MultitenantBlackBoxContext assertThat(VerifyAcknowledgements verifier) {
        Acknowledgements acks = output().commandAcks();
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
    public MultitenantBlackBoxContext assertThat(VerifyCommands verifier) {
        EmittedCommands commands = output().emittedCommands();
        verifier.verify(commands);
        return this;
    }

    /**
     * Does the same as {@link #assertThat(VerifyStateByTenant)}, but with a custom tenant ID.
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyState verifier) {
        QueryService queryService = queryService();
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
    public MultitenantBlackBoxContext assertThat(VerifyStateByTenant verifyByTenant) {
        VerifyState verifier = verifyByTenant.apply(tenantId);
        return assertThat(verifier);
    }

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    private EmittedEvents emittedEvents() {
        TenantAwareRunner tenantAwareRunner = TenantAwareRunner.with(tenantId);
        EmittedEvents events = tenantAwareRunner.evaluate(() -> output().emittedEvents());
        return events;
    }
}
