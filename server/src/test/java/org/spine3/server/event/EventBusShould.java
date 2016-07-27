/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.event.error.InvalidEventException;
import org.spine3.server.event.error.UnsupportedEventException;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.server.validate.MessageValidator;
import org.spine3.test.event.ProjectCreated;
import org.spine3.validate.ConstraintViolation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class EventBusShould {

    private EventStore eventStore;
    private EventBus eventBus;
    private TestResponseObserver responseObserver;

    @Before
    public void setUp() {
        setUp(null);
    }

    private void setUp(@Nullable EventEnricher enricher) {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        final EventStore.Builder storeBuilder = EventStore.newBuilder()
                .setStreamExecutor(MoreExecutors.directExecutor())
                .setStorage(storageFactory.createEventStorage())
                .setLogger(EventStore.log());
        this.eventStore = spy(storeBuilder.build());
        final EventBus.Builder busBuilder = EventBus.newBuilder()
                .setEventStore(eventStore);
        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        this.eventBus = busBuilder.build();
        this.responseObserver = new TestResponseObserver();
    }

    @Test
    public void have_builder() {
        assertNotNull(EventBus.newBuilder());
    }

    @Test
    public void return_associated_EventStore() {
        final EventBus result = EventBus.newBuilder().setEventStore(eventStore)
                                        .build();
        assertNotNull(result.getEventStore());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        // Pass just String instance.
        //noinspection EmptyClass
        eventBus.subscribe(new EventSubscriber() {});
    }

    @Test
    public void register_event_subscriber() {
        final EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
        final EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();

        eventBus.subscribe(subscriberOne);
        eventBus.subscribe(subscriberTwo);

        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        assertTrue(eventBus.hasSubscribers(eventClass));

        final Collection<EventSubscriber> subscribers = eventBus.getSubscribers(eventClass);
        assertTrue(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));
    }

    @Test
    public void unregister_subscribers() {
        final EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
        final EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();
        eventBus.subscribe(subscriberOne);
        eventBus.subscribe(subscriberTwo);
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.unsubscribe(subscriberOne);

        // Check that the 2nd subscriber with the same event subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<EventSubscriber> subscribers = eventBus.getSubscribers(eventClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        eventBus.unsubscribe(subscriberTwo);

        assertFalse(eventBus.getSubscribers(eventClass)
                            .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_event_posted() {
        final ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();
        final Event event = Given.Event.projectCreated();
        eventBus.subscribe(subscriber);

        eventBus.post(event);

        assertEquals(event, subscriber.getEventHandled());
    }

    @Test
    public void register_dispatchers() {
        final EventDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        assertTrue(eventBus.getDispatchers(EventClass.of(ProjectCreated.class)).contains(dispatcher));
    }

    @Test
    public void call_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        eventBus.post(Given.Event.projectCreated());

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void unregister_dispatchers() {
        final EventDispatcher dispatcherOne = new BareDispatcher();
        final EventDispatcher dispatcherTwo = new BareDispatcher();
        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        eventBus.register(dispatcherOne);
        eventBus.register(dispatcherTwo);

        eventBus.unregister(dispatcherOne);
        final Set<EventDispatcher> dispatchers = eventBus.getDispatchers(eventClass);

        // Check we don't have 1st dispatcher, but have 2nd.
        assertFalse(dispatchers.contains(dispatcherOne));
        assertTrue(dispatchers.contains(dispatcherTwo));

        eventBus.unregister(dispatcherTwo);
        assertFalse(eventBus.getDispatchers(eventClass)
                            .contains(dispatcherTwo));
    }

    @Test
    public void catch_exceptions_caused_by_subscribers() {
        final FaultySubscriber faultySubscriber = new FaultySubscriber();

        eventBus.subscribe(faultySubscriber);
        eventBus.post(Given.Event.projectCreated());

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Test
    public void assure_that_event_is_valid_and_subscriber_registered() {
        eventBus.subscribe(new ProjectCreatedSubscriber());

        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(), responseObserver);
        assertTrue(isValid);
        assertResponseIsOk(responseObserver);
    }

    @Test
    public void assure_that_event_is_valid_and_dispatcher_registered() {
        eventBus.register(new BareDispatcher());

        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(), responseObserver);
        assertTrue(isValid);
        assertResponseIsOk(responseObserver);
    }

    @Test
    public void call_onError_if_event_is_invalid() {
        final MessageValidator validator = mock(MessageValidator.class);
        doReturn(newArrayList(ConstraintViolation.getDefaultInstance()))
                .when(validator)
                .validate(any(Message.class));

        final EventBus eventBus = EventBus.newBuilder()
                                          .setEventStore(eventStore)
                                          .setEventValidator(validator)
                                          .build();
        eventBus.subscribe(new ProjectCreatedSubscriber());

        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(), responseObserver);

        assertFalse(isValid);
        assertReturnedExceptionAndNoResponse(InvalidEventException.class, responseObserver);
    }

    @Test
    public void call_onError_if_event_is_unsupported() {
        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(), responseObserver);

        assertFalse(isValid);
        assertReturnedExceptionAndNoResponse(UnsupportedEventException.class, responseObserver);
    }

    @Test
    public void unregister_registries_on_close() throws Exception {
        eventBus.register(new BareDispatcher());
        eventBus.subscribe(new ProjectCreatedSubscriber());
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.close();

        assertTrue(eventBus.getDispatchers(eventClass).isEmpty());
        assertTrue(eventBus.getSubscribers(eventClass).isEmpty());
        verify(eventStore).close();
    }

    @Test
    public void have_log() {
        assertNotNull(EventBus.log());
    }

    @Test
    public void do_not_have_Enricher_by_default() {
        assertNull(eventBus.getEnricher());
    }

    @Test
    public void enrich_event_if_it_can_be_enriched() {
        final EventEnricher enricher = mock(EventEnricher.class);
        final Event event = Given.Event.projectCreated();
        doReturn(true).when(enricher).canBeEnriched(any(Event.class));
        doReturn(event).when(enricher).enrich(any(Event.class));
        setUp(enricher);
        eventBus.subscribe(new ProjectCreatedSubscriber());

        eventBus.post(event);

        verify(enricher).enrich(any(Event.class));
    }

    @Test
    public void do_not_enrich_event_if_it_cannot_be_enriched() {
        final EventEnricher enricher = mock(EventEnricher.class);
        doReturn(false).when(enricher).canBeEnriched(any(Event.class));
        setUp(enricher);
        eventBus.subscribe(new ProjectCreatedSubscriber());

        eventBus.post(Given.Event.projectCreated());

        verify(enricher, never()).enrich(any(Event.class));
    }

    private static void assertResponseIsOk(TestResponseObserver responseObserver) {
        assertEquals(Responses.ok(), responseObserver.getResponse());
        assertTrue(responseObserver.isCompleted());
        assertNull(responseObserver.getThrowable());
    }

    private static void assertReturnedExceptionAndNoResponse(Class<? extends Exception> exceptionClass,
                                                             TestResponseObserver responseObserver) {
        final Throwable cause = responseObserver.getThrowable().getCause();
        assertEquals(exceptionClass, cause.getClass());
        assertNull(responseObserver.getResponse());
    }

    private static class ProjectCreatedSubscriber extends EventSubscriber {

        private Event eventHandled;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.eventHandled = Events.createEvent(event, context);
        }

        /* package */ Event getEventHandled() {
            return eventHandled;
        }
    }

    /** The subscriber which throws exception from the subscriber method. */
    private static class FaultySubscriber extends EventSubscriber {

        private boolean methodCalled = false;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            methodCalled = true;
            throw new UnsupportedOperationException("What did you expect from " +
                                                            FaultySubscriber.class.getSimpleName() + '?');
        }

        /* package */ boolean isMethodCalled() {
            return this.methodCalled;
        }
    }

    /** A simple dispatcher class, which only dispatch and does not have own event subscribing methods. */
    private static class BareDispatcher implements EventDispatcher {

        private boolean dispatchCalled = false;

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of(EventClass.of(ProjectCreated.class));
        }

        @Override
        public void dispatch(Event event) {
            dispatchCalled = true;
        }

        /* package */ boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    private static class TestResponseObserver implements StreamObserver<Response> {

        private Response response;
        private Throwable throwable;
        private boolean completed = false;

        @Override
        public void onNext(Response response) {
            this.response = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        /* package */ Response getResponse() {
            return response;
        }

        /* package */ Throwable getThrowable() {
            return throwable;
        }

        /* package */ boolean isCompleted() {
            return this.completed;
        }
    }
}
