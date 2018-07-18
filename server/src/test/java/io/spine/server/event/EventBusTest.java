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

package io.spine.server.event;

import com.google.protobuf.Int32Value;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.core.Events;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.given.EventBusTestEnv.BareDispatcher;
import io.spine.server.event.given.EventBusTestEnv.EBProjectArchivedSubscriber;
import io.spine.server.event.given.EventBusTestEnv.EBProjectCreatedNoOpSubscriber;
import io.spine.server.event.given.EventBusTestEnv.EBTaskAddedNoOpSubscriber;
import io.spine.server.event.given.EventBusTestEnv.GivenEvent;
import io.spine.server.event.given.EventBusTestEnv.ProjectCreatedSubscriber;
import io.spine.server.event.given.EventBusTestEnv.ProjectRepository;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.test.event.EBTaskAdded;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.Task;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.spine.core.BoundedContextNames.newName;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.event.given.EventBusTestEnv.TaskCreatedFilter;
import static io.spine.server.event.given.EventBusTestEnv.addTasks;
import static io.spine.server.event.given.EventBusTestEnv.command;
import static io.spine.server.event.given.EventBusTestEnv.createProject;
import static io.spine.server.event.given.EventBusTestEnv.eventBusBuilder;
import static io.spine.server.event.given.EventBusTestEnv.invalidArchiveProject;
import static io.spine.server.event.given.EventBusTestEnv.newTask;
import static io.spine.server.event.given.EventBusTestEnv.readEvents;
import static io.spine.testing.Verify.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names
@DisplayName("EventBus should")
public class EventBusTest {

    private EventBus eventBus;
    private CommandBus commandBus;
    private BoundedContext bc;

    private void setUp(@Nullable EventEnricher enricher) {
        EventBus.Builder eventBusBuilder = eventBusBuilder(enricher);

        bc = BoundedContext.newBuilder()
                           .setEventBus(eventBusBuilder)
                           .setMultitenant(true)
                           .build();

        ProjectRepository projectRepository = new ProjectRepository();
        bc.register(projectRepository);

        this.commandBus = bc.getCommandBus();
        this.eventBus = bc.getEventBus();
    }

    @BeforeEach
    void setUp() {
        setUp(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        bc.close();
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("have builder")
    void haveBuilder() {
        assertNotNull(EventBus.newBuilder());
    }

    @Test
    @DisplayName("return associated EventStore")
    void returnEventStore() {
        EventStore eventStore = mock(EventStore.class);
        EventBus result = EventBus.newBuilder()
                                  .setEventStore(eventStore)
                                  .build();
        assertEquals(eventStore, result.getEventStore());
    }

    @Test
    @DisplayName("reject object which has no subscriber methods")
    void rejectEmptySubscriber() {

        // Pass just String instance.
        assertThrows(IllegalArgumentException.class,
                     () -> eventBus.register(new EventSubscriber() {
                     }));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("event subscriber")
        void eventSubscriber() {
            EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
            EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();

            eventBus.register(subscriberOne);
            eventBus.register(subscriberTwo);

            EventClass eventClass = EventClass.of(ProjectCreated.class);
            assertTrue(eventBus.hasDispatchers(eventClass));

            Collection<? extends EventDispatcher<?>> dispatchers =
                    eventBus.getDispatchers(eventClass);
            assertTrue(dispatchers.contains(subscriberOne));
            assertTrue(dispatchers.contains(subscriberTwo));
        }

        @Test
        @DisplayName("event dispatcher")
        void eventDispatcher() {
            EventDispatcher dispatcher = new BareDispatcher();

            eventBus.register(dispatcher);

            assertTrue(eventBus.getDispatchers(EventClass.of(ProjectCreated.class))
                               .contains(dispatcher));
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("event subscriber")
        void eventSubscriber() {
            EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
            EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();
            eventBus.register(subscriberOne);
            eventBus.register(subscriberTwo);
            EventClass eventClass = EventClass.of(ProjectCreated.class);

            eventBus.unregister(subscriberOne);

            // Check that the 2nd subscriber with the same event subscriber method remains
            // after the 1st subscriber unregisters.
            Collection<? extends EventDispatcher<?>> subscribers =
                    eventBus.getDispatchers(eventClass);
            assertFalse(subscribers.contains(subscriberOne));
            assertTrue(subscribers.contains(subscriberTwo));

            // Check that after 2nd subscriber us unregisters he's no longer in
            eventBus.unregister(subscriberTwo);

            assertFalse(eventBus.getDispatchers(eventClass)
                                .contains(subscriberTwo));
        }

        @Test
        @DisplayName("event dispatcher")
        void eventDispatcher() {
            EventDispatcher dispatcherOne = new BareDispatcher();
            EventDispatcher dispatcherTwo = new BareDispatcher();
            EventClass eventClass = EventClass.of(ProjectCreated.class);
            eventBus.register(dispatcherOne);
            eventBus.register(dispatcherTwo);

            eventBus.unregister(dispatcherOne);
            Set<? extends EventDispatcher<?>> dispatchers = eventBus.getDispatchers(eventClass);

            // Check we don't have 1st dispatcher, but have 2nd.
            assertFalse(dispatchers.contains(dispatcherOne));
            assertTrue(dispatchers.contains(dispatcherTwo));

            eventBus.unregister(dispatcherTwo);
            assertFalse(eventBus.getDispatchers(eventClass)
                                .contains(dispatcherTwo));
        }
    }

    @Nested
    @DisplayName("call")
    class Call {

        @Test
        @DisplayName("event subscriber")
        void eventSubscriber() {
            ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();
            Event event = GivenEvent.projectCreated();
            eventBus.register(subscriber);

            eventBus.post(event);

            // Exclude event ID from comparison.
            assertEquals(Events.getMessage(event), subscriber.getEventMessage());
            assertEquals(event.getContext(), subscriber.getEventContext());
        }

        @Test
        @DisplayName("event dispatcher")
        void eventDispatcher() {
            BareDispatcher dispatcher = new BareDispatcher();

            eventBus.register(dispatcher);

            eventBus.post(GivenEvent.projectCreated());

            assertTrue(dispatcher.isDispatchCalled());
        }
    }

    @Test
    @DisplayName("unregister registries on close")
    void unregisterRegistriesOnClose() throws Exception {
        EventStore eventStore = spy(mock(EventStore.class));
        EventBus eventBus = EventBus
                .newBuilder()
                .setEventStore(eventStore)
                .build();
        eventBus.register(new BareDispatcher());
        eventBus.register(new ProjectCreatedSubscriber());
        EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.close();

        assertTrue(eventBus.getDispatchers(eventClass)
                           .isEmpty());
        verify(eventStore).close();
    }

    @Nested
    @DisplayName("manage event enrichment")
    class ManageEventEnrichment {

        @Test
        @DisplayName("for event that can be enriched")
        void forEnrichable() {
            EventEnricher enricher = mock(EventEnricher.class);
            EventEnvelope event = EventEnvelope.of(GivenEvent.projectCreated());
            doReturn(true).when(enricher)
                          .canBeEnriched(any(EventEnvelope.class));
            doReturn(event).when(enricher)
                           .enrich(any(EventEnvelope.class));

            // Close the bounded context, which was previously initialized in `@Before`.
            closeBoundedContext();
            setUp(enricher);
            eventBus.register(new ProjectCreatedSubscriber());

            eventBus.post(event.getOuterObject());

            verify(enricher).enrich(any(EventEnvelope.class));
        }

        @Test
        @DisplayName("for event that cannot be enriched")
        void forNonEnrichable() {
            EventEnricher enricher = mock(EventEnricher.class);
            doReturn(false).when(enricher)
                           .canBeEnriched(any(EventEnvelope.class));

            // Close the bounded context, which was previously initialized in `@Before`.
            closeBoundedContext();
            setUp(enricher);
            eventBus.register(new ProjectCreatedSubscriber());

            eventBus.post(GivenEvent.projectCreated());

            verify(enricher, never()).enrich(any(EventEnvelope.class));
        }
    }

    @Test
    @DisplayName("create validator once")
    void createValidatorOnce() {
        EnvelopeValidator<EventEnvelope> validator = eventBus.getValidator();
        assertNotNull(validator);
        assertSame(validator, eventBus.getValidator());
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("event")
        void event() {
            Command command = command(createProject());
            eventBus.register(new EBProjectCreatedNoOpSubscriber());
            commandBus.post(command, StreamObservers.noOpObserver());

            List<Event> events = readEvents(eventBus);
            assertSize(1, events);
        }

        @Test
        @DisplayName("dead event")
        void deadEvent() {
            Command command = command(createProject());

            commandBus.post(command, StreamObservers.noOpObserver());

            List<Event> events = readEvents(eventBus);
            assertSize(1, events);
        }

        /**
         * Ensures that events are stored when all of them pass the filters.
         *
         * <p> To filter the {@link EBTaskAdded} events the {@linkplain EventBus} has a custom
         * filter. The {@link TaskCreatedFilter} filters out {@link EBTaskAdded} events with
         * {@link Task#getDone()} set to {@code true}.
         *
         * <p> The {@link EBTaskAddedNoOpSubscriber} is registered so that the event would not get
         * filtered out by the {@link io.spine.server.bus.DeadMessageFilter}.
         */
        @Test
        @DisplayName("multiple events")
        void multipleEvents() {
            eventBus.register(new EBTaskAddedNoOpSubscriber());
            Command command = command(addTasks(newTask(false), newTask(false), newTask(false)));

            commandBus.post(command, StreamObservers.noOpObserver());

            List<Event> storedEvents = readEvents(eventBus);
            assertSize(3, storedEvents);
        }
    }

    @Nested
    @DisplayName("not store")
    class NotStore {

        @Test
        @DisplayName("invalid event")
        void invalidEvent() {
            Command command = command(invalidArchiveProject());
            eventBus.register(new EBProjectArchivedSubscriber());

            commandBus.post(command, StreamObservers.noOpObserver());

            List<Event> events = readEvents(eventBus);
            assertSize(0, events);
        }

        @Test
        @DisplayName("invalid dead event")
        void invalidDeadEvent() {
            Command command = command(invalidArchiveProject());

            commandBus.post(command, StreamObservers.noOpObserver());

            List<Event> events = readEvents(eventBus);
            assertSize(0, events);
        }

        /**
         * Ensures that events are not stored when none of them pass the filters.
         *
         * <p>To filter the {@link EBTaskAdded} events the {@linkplain EventBus} has a custom
         * filter. The {@link TaskCreatedFilter} filters out {@link EBTaskAdded} events with
         * {@link Task#getDone()} set to {@code true}.
         *
         * <p>The {@link EBTaskAddedNoOpSubscriber} is registered so that the event would not get
         * filtered out by the {@link io.spine.server.bus.DeadMessageFilter}.
         */
        @Test
        @DisplayName("multiple events failing filters")
        void multipleEventsFailingFilters() {
            eventBus.register(new EBTaskAddedNoOpSubscriber());
            Command command = command(addTasks(newTask(true), newTask(true), newTask(true)));

            commandBus.post(command, StreamObservers.noOpObserver());

            List<Event> storedEvents = readEvents(eventBus);
            assertSize(0, storedEvents);
        }
    }

    /**
     * Ensures that events which pass filters and the ones that don’t are treated independently when
     * sent in batch.
     *
     * <p> To filter the {@link EBTaskAdded} events the {@linkplain EventBus} has a custom filter.
     * The {@link TaskCreatedFilter} filters out {@link EBTaskAdded} events with
     * {@link Task#getDone()} set to {@code true}.
     *
     * <p> The {@link EBTaskAddedNoOpSubscriber} is registered so that the event would not get
     * filtered out by the {@link io.spine.server.bus.DeadMessageFilter}.
     */
    @Test
    @DisplayName("store only events passing filters")
    void filterOutFailingEvents() {
        eventBus.register(new EBTaskAddedNoOpSubscriber());
        Command command = command(addTasks(newTask(false), newTask(true), newTask(false),
                                           newTask(true), newTask(true)));

        commandBus.post(command, StreamObservers.noOpObserver());

        List<Event> storedEvents = readEvents(eventBus);
        assertSize(2, storedEvents);

        for (Event event : storedEvents) {
            EBTaskAdded contents = unpack(event.getMessage());
            Task task = contents.getTask();
            assertFalse(task.getDone());
        }
    }

    /**
     * Tests the concurrent access to the {@linkplain io.spine.server.bus.BusFilter bus filters}.
     *
     * <p>The {@linkplain io.spine.server.bus.FilterChain filter chain} is a queue of the filters
     * which are sequentially applied to the posted message. The first {@code Bus.post()} call
     * invokes the filters lazy initialization. In the concurrent environment (which is natural for
     * a {@link io.spine.server.bus.Bus Bus}), the initialization may be performed multiple times.
     * Thus, some unexpected issues may appear when accessing the non-synchronously initialized
     * filter chain.
     *
     * <p>To make sure that the chain works fine (i.e. produces no exceptions), we invoke the
     * initialization multiple times from several threads.
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "BusyWait"}) // OK for such test case.
    @Disabled // This test is used only to diagnose EventBus malfunctions in concurrent environment.
    // It's too long to execute this test per each build, so we leave it as is for now.
    // Please see build log to find out if there were some errors during the test execution.
    @Test
    @DisplayName("store filters regarding possible concurrent modifications")
    void storeFiltersInConcurrentEnv() throws InterruptedException {
        int threadCount = 50;

        // "Random" more or less valid Event.
        Event event = Event
                .newBuilder()
                .setId(EventId.newBuilder()
                              .setValue("123-1"))
                .setMessage(pack(Int32Value.newBuilder()
                                           .setValue(42)
                                           .build()))
                .build();
        StorageFactory storageFactory =
                StorageFactorySwitch.newInstance(newName("baz"), false)
                                    .get();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Catch non-easily reproducible bugs.
        for (int i = 0; i < 300; i++) {
            EventBus eventBus = EventBus
                    .newBuilder()
                    .setStorageFactory(storageFactory)
                    .build();
            for (int j = 0; j < threadCount; j++) {
                executor.execute(() -> eventBus.post(event));
            }
            // Let the system destroy all the native threads, clean up, etc.
            Thread.sleep(100);
        }
        executor.shutdownNow();
    }

    private void closeBoundedContext() {
        try {
            bc.close();
        } catch (Exception ignored) {
        }
    }
}
