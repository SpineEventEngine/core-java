/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.delivery.Consumers;
import io.spine.server.event.given.EventBusTestEnv.GivenEvent;
import io.spine.server.storage.StorageFactory;
import io.spine.test.event.ProjectCreated;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class EventBusShould {

    private EventBus eventBus;
    private EventBus eventBusWithPosponedExecution;
    private PostponedDispatcherEventDelivery postponedDispatcherDelivery;
    private Executor delegateDispatcherExecutor;
    private StorageFactory storageFactory;

    @Before
    public void setUp() {
        setUp(null);
    }

    private void setUp(@Nullable EventEnricher enricher) {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(true)
                                                .build();
        this.storageFactory = bc.getStorageFactory();
        /**
         * Cannot use {@link com.google.common.util.concurrent.MoreExecutors#directExecutor()
         * MoreExecutors.directExecutor()} because it's impossible to spy on {@code final} classes.
         */
        this.delegateDispatcherExecutor = spy(directExecutor());
        this.postponedDispatcherDelivery =
                new PostponedDispatcherEventDelivery(delegateDispatcherExecutor);
        buildEventBus(enricher);
        buildEventBusWithPostponedExecution(enricher);
    }

    @SuppressWarnings("MethodMayBeStatic")   /* it cannot, as its result is used in {@code org.mockito.Mockito.spy() */
    private Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private void buildEventBusWithPostponedExecution(@Nullable EventEnricher enricher) {
        final EventBus.Builder busBuilder =
                EventBus.newBuilder()
                        .setStorageFactory(storageFactory)
                        .setDispatcherEventDelivery(postponedDispatcherDelivery);

        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        this.eventBusWithPosponedExecution = busBuilder.build();
    }

    private void buildEventBus(@Nullable EventEnricher enricher) {
        final EventBus.Builder busBuilder = EventBus.newBuilder()
                                                    .setStorageFactory(storageFactory);
        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        this.eventBus = busBuilder.build();
    }

    @Test
    public void have_builder() {
        assertNotNull(EventBus.newBuilder());
    }

    @Test
    public void return_associated_EventStore() {
        final EventStore eventStore = mock(EventStore.class);
        final EventBus result = EventBus.newBuilder()
                                        .setEventStore(eventStore)
                                        .build();
        assertEquals(eventStore, result.getEventStore());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        // Pass just String instance.
        eventBus.register(new EventSubscriber() {
        });
    }

    @Test
    public void register_event_subscriber() {
        final EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
        final EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();

        eventBus.register(subscriberOne);
        eventBus.register(subscriberTwo);

        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        assertTrue(eventBus.hasDispatchers(eventClass));

        final Collection<? extends EventDispatcher<?>> dispatchers =
                eventBus.getDispatchers(eventClass);
        assertTrue(dispatchers.contains(subscriberOne));
        assertTrue(dispatchers.contains(subscriberTwo));
    }

    @Test
    public void unregister_subscribers() {
        final EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
        final EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();
        eventBus.register(subscriberOne);
        eventBus.register(subscriberTwo);
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.unregister(subscriberOne);

        // Check that the 2nd subscriber with the same event subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<? extends EventDispatcher<?>> subscribers =
                eventBus.getDispatchers(eventClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        eventBus.unregister(subscriberTwo);

        assertFalse(eventBus.getDispatchers(eventClass)
                            .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_event_posted() {
        final ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();
        final Event event = GivenEvent.projectCreated();
        eventBus.register(subscriber);

        eventBus.post(event);

        // Exclude event ID from comparison.
        assertEquals(Events.getMessage(event), subscriber.getEventMessage());
        assertEquals(event.getContext(), subscriber.getEventContext());
    }

    @Test
    public void register_dispatchers() {
        final EventDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        assertTrue(eventBus.getDispatchers(EventClass.of(ProjectCreated.class))
                           .contains(dispatcher));
    }

    @Test
    public void call_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        eventBus.post(GivenEvent.projectCreated());

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void return_direct_DispatcherDelivery_if_none_customized() {
        final DispatcherEventDelivery actual = eventBus.delivery();
        assertTrue(actual instanceof DispatcherEventDelivery.DirectDelivery);
    }

    @Test
    public void not_call_dispatchers_if_dispatcher_event_execution_postponed() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBusWithPosponedExecution.register(dispatcher);

        final Event event = GivenEvent.projectCreated();
        eventBusWithPosponedExecution.post(event);
        assertFalse(dispatcher.isDispatchCalled());

        final boolean eventPostponed = postponedDispatcherDelivery.isPostponed(event, dispatcher);
        assertTrue(eventPostponed);
    }

    @Test
    public void deliver_postponed_event_to_dispatcher_using_configured_executor() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBusWithPosponedExecution.register(dispatcher);

        final Event event = GivenEvent.projectCreated();
        eventBusWithPosponedExecution.post(event);
        final Set<EventEnvelope> postponedEvents = postponedDispatcherDelivery.getPostponedEvents();
        final EventEnvelope postponedEvent = postponedEvents.iterator()
                                                            .next();
        verify(delegateDispatcherExecutor, never()).execute(any(Runnable.class));
        postponedDispatcherDelivery.deliverNow(postponedEvent, Consumers.idOf(dispatcher));
        assertTrue(dispatcher.isDispatchCalled());
        verify(delegateDispatcherExecutor).execute(any(Runnable.class));
    }

    @Test
    public void pick_proper_consumer_by_consumer_id_when_delivering_to_delegates_of_same_event() {
        final FirstProjectCreatedDelegate first = new FirstProjectCreatedDelegate();
        final AnotherProjectCreatedDelegate second = new AnotherProjectCreatedDelegate();

        final DelegatingEventDispatcher<String> firstDispatcher =
                DelegatingEventDispatcher.of(first);
        final DelegatingEventDispatcher<String> secondDispatcher =
                DelegatingEventDispatcher.of(second);

        eventBusWithPosponedExecution.register(firstDispatcher);
        eventBusWithPosponedExecution.register(secondDispatcher);

        final Event event = GivenEvent.projectCreated();
        eventBusWithPosponedExecution.post(event);
        final Set<EventEnvelope> postponedEvents = postponedDispatcherDelivery.getPostponedEvents();
        final EventEnvelope postponedEvent = postponedEvents.iterator()
                                                            .next();
        verify(delegateDispatcherExecutor, never()).execute(any(Runnable.class));
        postponedDispatcherDelivery.deliverNow(postponedEvent, Consumers.idOf(firstDispatcher));
        assertTrue(first.isDispatchCalled());
        verify(delegateDispatcherExecutor).execute(any(Runnable.class));
        assertFalse(second.isDispatchCalled());
    }

    @Test
    public void unregister_dispatchers() {
        final EventDispatcher dispatcherOne = new BareDispatcher();
        final EventDispatcher dispatcherTwo = new BareDispatcher();
        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        eventBus.register(dispatcherOne);
        eventBus.register(dispatcherTwo);

        eventBus.unregister(dispatcherOne);
        final Set<? extends EventDispatcher<?>> dispatchers = eventBus.getDispatchers(eventClass);

        // Check we don't have 1st dispatcher, but have 2nd.
        assertFalse(dispatchers.contains(dispatcherOne));
        assertTrue(dispatchers.contains(dispatcherTwo));

        eventBus.unregister(dispatcherTwo);
        assertFalse(eventBus.getDispatchers(eventClass)
                            .contains(dispatcherTwo));
    }

    @Test
    public void unregister_registries_on_close() throws Exception {
        final EventStore eventStore = spy(mock(EventStore.class));
        final EventBus eventBus = EventBus.newBuilder()
                                          .setEventStore(eventStore)
                                          .build();
        eventBus.register(new BareDispatcher());
        eventBus.register(new ProjectCreatedSubscriber());
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.close();

        assertTrue(eventBus.getDispatchers(eventClass)
                           .isEmpty());
        verify(eventStore).close();
    }

    @Test
    public void enrich_event_if_it_can_be_enriched() {
        final EventEnricher enricher = mock(EventEnricher.class);
        final EventEnvelope event = EventEnvelope.of(GivenEvent.projectCreated());
        doReturn(true).when(enricher)
                      .canBeEnriched(any(EventEnvelope.class));
        doReturn(event).when(enricher)
                       .enrich(any(EventEnvelope.class));
        setUp(enricher);
        eventBus.register(new ProjectCreatedSubscriber());

        eventBus.post(event.getOuterObject());

        verify(enricher).enrich(any(EventEnvelope.class));
    }

    @Test
    public void do_not_enrich_event_if_it_cannot_be_enriched() {
        final EventEnricher enricher = mock(EventEnricher.class);
        doReturn(false).when(enricher)
                       .canBeEnriched(any(EventEnvelope.class));
        setUp(enricher);
        eventBus.register(new ProjectCreatedSubscriber());

        eventBus.post(GivenEvent.projectCreated());

        verify(enricher, never()).enrich(any(EventEnvelope.class));
    }

    @Test
    public void create_validator_once() {
        final EnvelopeValidator<EventEnvelope> validator = eventBus.getValidator();
        assertNotNull(validator);
        assertSame(validator, eventBus.getValidator());
    }

    private static class ProjectCreatedSubscriber extends EventSubscriber {

        private Message eventMessage;
        private EventContext eventContext;

        @Subscribe
        public void on(ProjectCreated eventMsg, EventContext context) {
            this.eventMessage = eventMsg;
            this.eventContext = context;
        }

        public Message getEventMessage() {
            return eventMessage;
        }

        public EventContext getEventContext() {
            return eventContext;
        }
    }

    /**
     * A simple dispatcher class, which only dispatch and does not have own event
     * subscribing methods.
     */
    private static class BareDispatcher implements EventDispatcher<String> {

        private boolean dispatchCalled = false;

        @Override
        public Set<EventClass> getMessageClasses() {
            return ImmutableSet.of(EventClass.of(ProjectCreated.class));
        }

        @Override
        public Set<String> dispatch(EventEnvelope event) {
            dispatchCalled = true;
            return Identity.of(this);
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        private boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    private static class PostponedDispatcherEventDelivery extends DispatcherEventDelivery {

        private final Map<EventEnvelope,
                Class<? extends EventDispatcher>> postponedExecutions = newHashMap();

        private PostponedDispatcherEventDelivery(Executor delegate) {
            super(delegate);
        }

        @Override
        public boolean shouldPostponeDelivery(EventEnvelope event, EventDispatcher consumer) {
            postponedExecutions.put(event, consumer.getClass());
            return true;
        }

        private boolean isPostponed(Event event, EventDispatcher dispatcher) {
            final EventEnvelope envelope = EventEnvelope.of(event);
            final Class<? extends EventDispatcher> actualClass = postponedExecutions.get(envelope);
            final boolean eventPostponed = actualClass != null;
            final boolean dispatcherMatches = eventPostponed && dispatcher.getClass()
                                                                          .equals(actualClass);
            return dispatcherMatches;
        }

        private Set<EventEnvelope> getPostponedEvents() {
            final Set<EventEnvelope> envelopes = postponedExecutions.keySet();
            return envelopes;
        }
    }

    /**
     * A delegate, dispatching {@link ProjectCreated} events.
     */
    private static class FirstProjectCreatedDelegate implements EventDispatcherDelegate<String> {
        private boolean dispatchCalled = false;

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of(EventClass.of(ProjectCreated.class));
        }

        @Override
        public Set<String> dispatchEvent(EventEnvelope envelope) {
            dispatchCalled = true;
            return ImmutableSet.of(toString());
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        private boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    /**
     * Another delegate, dispatching {@link ProjectCreated} events.
     */
    private static class AnotherProjectCreatedDelegate implements EventDispatcherDelegate<String> {
        private boolean dispatchCalled = false;

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of(EventClass.of(ProjectCreated.class));
        }

        @Override
        public Set<String> dispatchEvent(EventEnvelope envelope) {
            dispatchCalled = true;
            return ImmutableSet.of(toString());
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        private boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }
}
