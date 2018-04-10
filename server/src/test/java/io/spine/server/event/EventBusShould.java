/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.core.Events;
import io.spine.core.Subscribe;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.given.EventBusTestEnv.GivenEvent;
import io.spine.server.event.given.EventBusTestEnv.ProjectRepository;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.test.event.EBProjectArchived;
import io.spine.test.event.EBProjectCreated;
import io.spine.test.event.EBTaskAdded;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.BoundedContext.newName;
import static io.spine.server.event.given.EventBusTestEnv.TaskCreatedFilter;
import static io.spine.server.event.given.EventBusTestEnv.addTasks;
import static io.spine.server.event.given.EventBusTestEnv.command;
import static io.spine.server.event.given.EventBusTestEnv.createProject;
import static io.spine.server.event.given.EventBusTestEnv.invalidArchiveProject;
import static io.spine.server.event.given.EventBusTestEnv.newTask;
import static io.spine.server.event.given.EventBusTestEnv.readEvents;
import static io.spine.test.Verify.assertSize;
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

/**
 * @author Mykhailo Drachuk
 */
public class EventBusShould {

    private EventBus eventBus;
    private CommandBus commandBus;
    private BoundedContext bc;

    @Before
    public void setUp() {
        setUp(null);
    }

    private void setUp(@Nullable EventEnricher enricher) {
        final EventBus.Builder eventBusBuilder = eventBusBuilder(enricher);

        bc = BoundedContext.newBuilder()
                           .setEventBus(eventBusBuilder)
                           .setMultitenant(true)
                           .build();

        final ProjectRepository projectRepository = new ProjectRepository();
        bc.register(projectRepository);

        this.commandBus = bc.getCommandBus();
        this.eventBus = bc.getEventBus();
    }

    @After
    public void tearDown() throws Exception {
        bc.close();
    }

    private static EventBus.Builder eventBusBuilder(@Nullable EventEnricher enricher) {
        final EventBus.Builder busBuilder = EventBus.newBuilder()
                                                    .appendFilter(new TaskCreatedFilter());
        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        return busBuilder;
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

        // Close the bounded context, which was previously initialized in `@Before`.
        closeBoundedContext();
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

        // Close the bounded context, which was previously initialized in `@Before`.
        closeBoundedContext();
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

    @Test
    public void store_an_event() {
        final Command command = command(createProject());
        eventBus.register(new EBProjectCreatedNoOpSubscriber());

        System.out.println("JUnit test: " + Thread.currentThread().getId());
        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> events = readEvents(eventBus);
        assertSize(1, events);
    }

    @Test
    public void store_a_dead_event() {
        final Command command = command(createProject());

        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> events = readEvents(eventBus);
        assertSize(1, events);
    }

    @Test
    public void not_store_an_invalid_event() {
        final Command command = command(invalidArchiveProject());
        eventBus.register(new EBProjectArchivedSubscriber());

        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> events = readEvents(eventBus);
        assertSize(0, events);
    }

    @Test
    public void not_store_an_invalid_dead_event() {
        final Command command = command(invalidArchiveProject());

        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> events = readEvents(eventBus);
        assertSize(0, events);
    }

    /**
     * Ensures that events are stored when all of them pass the filters.
     *
     * <p> To filter the {@link EBTaskAdded} events the {@linkplain EventBus} has a custom filter. 
     * The {@link TaskCreatedFilter} filters out {@link EBTaskAdded} events with 
     * {@link Task#getDone()} set to {@code true}.
     *
     * <p> The {@link EBTaskAddedNoOpSubscriber} is registered so that the event would not get 
     * filtered out by the {@link io.spine.server.bus.DeadMessageFilter}.
     */
    @Test
    public void store_multiple_messages_passing_filters() {
        eventBus.register(new EBTaskAddedNoOpSubscriber());
        final Command command = command(addTasks(newTask(false), newTask(false), newTask(false)));

        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> storedEvents = readEvents(eventBus);
        assertSize(3, storedEvents);
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
     * 
     */
    @Test
    public void store_only_events_passing_filters() {
        eventBus.register(new EBTaskAddedNoOpSubscriber());
        final Command command = command(addTasks(newTask(false), newTask(true), newTask(false),
                                                 newTask(true), newTask(true)));

        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> storedEvents = readEvents(eventBus);
        assertSize(2, storedEvents);

        for (Event event : storedEvents) {
            final EBTaskAdded contents = unpack(event.getMessage());
            final Task task = contents.getTask();
            assertFalse(task.getDone());
        }
    }

    /**
     * Ensures that events are not stored when none of them pass the filters.
     *
     * <p> To filter the {@link EBTaskAdded} events the {@linkplain EventBus} has a custom filter. 
     * The {@link TaskCreatedFilter} filters out {@link EBTaskAdded} events with 
     * {@link Task#getDone()} set to {@code true}.
     *
     * <p> The {@link EBTaskAddedNoOpSubscriber} is registered so that the event would not get 
     * filtered out by the {@link io.spine.server.bus.DeadMessageFilter}.
     */
    @Test
    public void not_store_any_events_when_they_are_failing_filtering() {
        eventBus.register(new EBTaskAddedNoOpSubscriber());
        final Command command = command(addTasks(newTask(true), newTask(true), newTask(true)));

        commandBus.post(command, StreamObservers.<Ack>noOpObserver());

        final List<Event> storedEvents = readEvents(eventBus);
        assertSize(0, storedEvents);
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
    @SuppressWarnings("MethodWithMultipleLoops") // OK for such test case.
    @Ignore // This test is used only to diagnose EventBus malfunctions in concurrent environment.
            // It's too long to execute this test per each build, so we leave it as is for now.
            // Please see build log to find out if there were some errors during the test execution.
    @Test
    public void store_filters_regarding_possible_concurrent_modifications()
            throws InterruptedException {
        final int threadCount = 50;

        // "Random" more or less valid Event.
        final Event event = Event.newBuilder()
                                 .setId(EventId.newBuilder().setValue("123-1"))
                                 .setMessage(pack(Int32Value.newBuilder()
                                                            .setValue(42)
                                                            .build()))
                                 .build();
        final StorageFactory storageFactory =
                StorageFactorySwitch.newInstance(newName("baz"), false).get();
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Catch non-easily reproducible bugs.
        for (int i = 0; i < 300; i++) {
            final EventBus eventBus = EventBus.newBuilder()
                                              .setStorageFactory(storageFactory)
                                              .build();
            for (int j = 0; j < threadCount; j++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        eventBus.post(event);
                    }
                });
            }
            // Let the system destroy all the native threads, clean up, etc.
            Thread.sleep(100);
        }
        executor.shutdownNow();
    }

    /**
     * {@link EBProjectCreated} subscriber that does nothing. Can be used for the event to get pass the
     * {@link io.spine.server.bus.DeadMessageFilter}.
     */
    private static class EBProjectCreatedNoOpSubscriber extends EventSubscriber {

        @Subscribe
        public void on(EBProjectCreated message, EventContext context) {
            // Do nothing.
        }
    }

    
    private static class EBProjectArchivedSubscriber extends EventSubscriber {

        private Message eventMessage;

        @Subscribe
        public void on(EBProjectArchived message, EventContext ignored) {
            this.eventMessage = message;
        }

        public Message getEventMessage() {
            return eventMessage;
        }
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

        private EventContext getEventContext() {
            return eventContext;
        }
    }

    /**
     * {@link EBTaskAdded} subscriber that does nothing. Can be used for the event to get pass the
     * {@link io.spine.server.bus.DeadMessageFilter}.
     */
    private static class EBTaskAddedNoOpSubscriber extends EventSubscriber {

        @Subscribe
        public void on(EBTaskAdded message, EventContext context) {
            // Do nothing.
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

    private void closeBoundedContext() {
        try {
            bc.close();
        } catch (Exception ignored) {
        }
    }
}
