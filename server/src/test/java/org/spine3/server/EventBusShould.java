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

package org.spine3.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.stream.EventStore;
import org.spine3.server.util.EventRecords;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.testdata.TestEventFactory;
import org.spine3.type.EventClass;

import java.util.Set;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class EventBusShould {

    private EventStore eventStore;
    private EventBus eventBus;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        this.eventStore = EventStore.newBuilder()
                .setStreamExecutor(MoreExecutors.directExecutor())
                .setStorage(storageFactory.createEventStorage())
                .setLogger(EventStore.log())
                .build();
        this.eventBus = EventBus.newInstance(eventStore, MoreExecutors.directExecutor());
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
        eventBus.register("reject_object_with_no_subscriber_methods");
    }

    /**
     * A simple one subscriber method handler class used in tests below.
     */
    private static class ProjectCreatedHandler {

        private boolean methodCalled = false;

        @SuppressWarnings("UnusedParameters") // OK for the test class.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.methodCalled = true;
        }

        /* package */ boolean isMethodCalled() {
            return methodCalled;
        }
    }

    @Test
    public void register_event_handler() {
        final Object handlerOne = new ProjectCreatedHandler();
        final Object handlerTwo = new ProjectCreatedHandler();

        eventBus.register(handlerOne);
        eventBus.register(handlerTwo);

        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        assertTrue(eventBus.hasSubscribers(eventClass));

        final Set<Object> subscribers = eventBus.getHandlers(eventClass);
        assertTrue(subscribers.contains(handlerOne));
        assertTrue(subscribers.contains(handlerTwo));
    }

    @Test
    public void unregister_handlers() {
        final Object handlerOne = new ProjectCreatedHandler();
        final Object handlerTwo = new ProjectCreatedHandler();
        eventBus.register(handlerOne);
        eventBus.register(handlerTwo);
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.unregister(handlerOne);

        // Check that the 2nd subscriber with the same event handling method remains
        // after the 1st subscriber unregisters.
        final Set<Object> subscribers = eventBus.getHandlers(eventClass);
        assertFalse(subscribers.contains(handlerOne));
        assertTrue(subscribers.contains(handlerTwo));

        // Check that after 2nd handler us unregisters he's no longer in
        eventBus.unregister(handlerTwo);

        assertFalse(eventBus.getHandlers(eventClass).contains(handlerTwo));
    }

    @Test
    public void call_subscribers_when_event_record_posted() {
        final ProjectCreatedHandler handler = new ProjectCreatedHandler();

        eventBus.register(handler);

        final Message createProject = TestEventFactory.projectCreatedEvent("call_subscribers_when_event_record_posted");
        final EventRecord record = EventRecords.createEventRecord(createProject, EventContext.getDefaultInstance());
        eventBus.post(record);

        assertTrue(handler.isMethodCalled());
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
        public void dispatch(Message event, EventContext context) {
            dispatchCalled = true;
        }

        /* package */ boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    @Test
    public void register_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        assertTrue(eventBus.getDispatchers(EventClass.of(ProjectCreated.class)).contains(dispatcher));
    }

    @Test
    public void call_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        final Message createProject = TestEventFactory.projectCreatedEvent("call_dispatchers");
        final EventRecord record = EventRecords.createEventRecord(createProject, EventContext.getDefaultInstance());
        eventBus.post(record);

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void unregister_dispatchers() {
        final BareDispatcher dispatcherOne = new BareDispatcher();
        final BareDispatcher dispatcherTwo = new BareDispatcher();
        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        eventBus.register(dispatcherOne);
        eventBus.register(dispatcherTwo);

        eventBus.unregister(dispatcherOne);
        final Set<EventDispatcher> dispatchers = eventBus.getDispatchers(eventClass);

        // Check we don't have 1st dispatcher, but have 2nd.
        assertFalse(dispatchers.contains(dispatcherOne));
        assertTrue(dispatchers.contains(dispatcherTwo));

        eventBus.unregister(dispatcherTwo);
        assertFalse(eventBus.getDispatchers(eventClass).contains(dispatcherTwo));
    }


    @Test
    public void catches_exceptions_caused_by_handlers() {
        final FaultyHandler faultyHandler = new FaultyHandler();

        eventBus.register(faultyHandler);
        final Message createProject = TestEventFactory.projectCreatedEvent("catches_exceptions_caused_by_handlers");
        final EventRecord record = EventRecords.createEventRecord(createProject, EventContext.getDefaultInstance());
        eventBus.post(record);

        assertTrue(faultyHandler.isMethodCalled());
    }

    /**
     * The handler which throws exception from the subscriber method.
     */
    private static class FaultyHandler {

        private boolean methodCalled = false;

        @SuppressWarnings("UnusedParameters")
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            methodCalled = true;
            throw new UnsupportedOperationException("What did you expect from FaultyHandler?");
        }

        /* package */ boolean isMethodCalled() {
            return this.methodCalled;
        }
    }

    @Test
    public void unregister_registries_on_close() throws Exception {
        eventBus.register(new BareDispatcher());
        eventBus.register(new ProjectCreatedHandler());
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.close();

        assertTrue(eventBus.getDispatchers(eventClass).isEmpty());
        assertTrue(eventBus.getHandlers(eventClass).isEmpty());
    }

    //TODO:2016-01-27:alexander.yevsyukov: Check that EventStore is closed on close() too. Using Mocks?

    //TODO:2016-01-27:alexander.yevsyukov: Reach 100% coverage.
}
