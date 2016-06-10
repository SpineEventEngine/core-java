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
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.event.error.InvalidEventException;
import org.spine3.server.event.error.UnsupportedEventException;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.server.validate.MessageValidator;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.testdata.TestEventFactory;
import org.spine3.validate.ConstraintViolation;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.spine3.testdata.TestEventMessageFactory.projectCreatedMsg;

@SuppressWarnings("InstanceMethodNamingConvention")
public class EventBusShould {

    //TODO:2016-01-27:alexander.yevsyukov: Check that EventStore is closed on close() too. Using Mocks?

    //TODO:2016-01-27:alexander.yevsyukov: Reach 100% coverage.

    private EventStore eventStore;
    private EventBus eventBus;
    private TestResponseObserver responseObserver;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        this.eventStore = EventStore.newBuilder()
                .setStreamExecutor(MoreExecutors.directExecutor())
                .setStorage(storageFactory.createEventStorage())
                .setLogger(EventStore.log())
                .build();
        this.eventBus = EventBus.newInstance(eventStore, MoreExecutors.directExecutor());
        this.responseObserver = new TestResponseObserver();
    }

    @Test
    public void create_direct_executor_instance() {
        assertNotNull(EventBus.newInstance(eventStore));
    }

    @Test
    public void create_instance_with_executor() {
        assertNotNull(EventBus.newInstance(eventStore, Executors.newSingleThreadExecutor()));
    }

    @Test
    public void return_associated_EventStore() {
        final EventBus bus = EventBus.newInstance(eventStore);
        assertNotNull(bus.getEventStore());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        // Pass just String instance.
        //noinspection EmptyClass
        eventBus.subscribe(new EventSubscriber() {});
    }

    /**
     * A simple subscriber class used in tests below.
     */
    private static class ProjectCreatedSubscriber extends EventSubscriber {

        private boolean methodCalled = false;

        // OK for the test class.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.methodCalled = true;
        }

        /* package */ boolean isMethodCalled() {
            return methodCalled;
        }
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
    public void call_subscribers_when_event_posted() {
        final ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();

        eventBus.subscribe(subscriber);
        eventBus.post(TestEventFactory.projectCreatedEvent());

        assertTrue(subscriber.isMethodCalled());
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

        eventBus.post(TestEventFactory.projectCreatedEvent());

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
    public void catches_exceptions_caused_by_subscribers() {
        final FaultySubscriber faultySubscriber = new FaultySubscriber();

        eventBus.subscribe(faultySubscriber);
        eventBus.post(TestEventFactory.projectCreatedEvent());

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Test
    public void return_ok_response_if_event_is_valid() {
        eventBus.subscribe(new TestEventSubscriber());

        final boolean isValid = eventBus.validate(projectCreatedMsg(), responseObserver);
        assertTrue(isValid);
        assertEquals(Responses.ok(), responseObserver.getResponse());
        assertTrue(responseObserver.isCompleted());
        assertNull(responseObserver.getThrowable());
    }

    @Test
    public void call_onError_if_event_is_invalid() {
        eventBus.subscribe(new TestEventSubscriber());
        final MessageValidator validator = mock(MessageValidator.class);
        doReturn(newArrayList(ConstraintViolation.getDefaultInstance()))
                .when(validator)
                .validate(any(Message.class));
        eventBus.setMessageValidator(validator);

        final boolean isValid = eventBus.validate(projectCreatedMsg(), responseObserver);

        assertFalse(isValid);
        final Throwable cause = responseObserver.getThrowable().getCause();
        assertEquals(InvalidEventException.class, cause.getClass());
        assertNull(responseObserver.getResponse());
    }

    @Test
    public void call_onError_if_event_is_unsupported() {
        final boolean isValid = eventBus.validate(projectCreatedMsg(), responseObserver);

        assertFalse(isValid);
        final Throwable cause = responseObserver.getThrowable().getCause();
        assertEquals(UnsupportedEventException.class, cause.getClass());
        assertNull(responseObserver.getResponse());
    }

    private static class TestEventSubscriber extends EventSubscriber {

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
        }
    }

    /**
     * The subscriber which throws exception from the subscriber method.
     */
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

    /**
     * A simple dispatcher class, which only dispatch and does not have own event subscribing methods.
     */
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

    @Test
    public void unregister_registries_on_close() throws Exception {
        eventBus.register(new BareDispatcher());
        eventBus.subscribe(new ProjectCreatedSubscriber());
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.close();

        assertTrue(eventBus.getDispatchers(eventClass).isEmpty());
        assertTrue(eventBus.getSubscribers(eventClass).isEmpty());
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
