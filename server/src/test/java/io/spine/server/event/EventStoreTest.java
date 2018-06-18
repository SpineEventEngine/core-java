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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.RejectionContext;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.TaskAdded;
import io.spine.testdata.Sample;
import io.spine.time.Durations2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.given.GivenEnrichment.withOneAttribute;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.test.Verify.assertSize;
import static io.spine.type.TypeName.of;
import static io.spine.validate.Validate.isDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("EventStore should")
class EventStoreTest {

    private static TestEventFactory eventFactory = null;

    private EventStore eventStore;

    private static EventStore createStore() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(false)
                                                .build();
        return EventStore.newBuilder()
                         .setStorageFactory(bc.getStorageFactory())
                         .setStreamExecutor(MoreExecutors.directExecutor())
                         .withDefaultLogger()
                         .build();
    }

    @BeforeAll
    static void prepare() {
        eventFactory = TestEventFactory.newInstance(EventStoreTest.class);
    }

    @BeforeEach
    void setUp() {
        eventStore = createStore();
    }

    @Test
    @DisplayName("read events by time bounds")
    void readEventsByTimeBounds() {
        final Duration delta = Durations2.seconds(111);
        final Timestamp present = getCurrentTime();
        final Timestamp past = subtract(present, delta);
        final Timestamp future = add(present, delta);

        final Event eventInPast = projectCreated(past);
        final Event eventInPresent = projectCreated(present);
        final Event eventInFuture = projectCreated(future);

        eventStore.append(eventInPast);
        eventStore.append(eventInPresent);
        eventStore.append(eventInFuture);

        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(past)
                                                       .setBefore(future)
                                                       .build();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Collection<Event> resultEvents = newConcurrentHashSet();
        eventStore.read(query, new ResponseObserver(resultEvents, done));
        assertDone(done);

        assertSize(1, resultEvents);
        final Event event = resultEvents.iterator()
                                        .next();
        assertEquals(eventInPresent, event);
    }

    @Test
    @DisplayName("read events by time and type")
    void readEventsByTimeAndType() {
        final Duration delta = Durations2.seconds(111);
        final Timestamp present = getCurrentTime();
        final Timestamp past = subtract(present, delta);
        final Timestamp future = add(present, delta);

        final Event eventInPast = taskAdded(past);
        final Event eventInPresent = projectCreated(present);
        final Event eventInFuture = taskAdded(future);

        eventStore.append(eventInPast);
        eventStore.append(eventInPresent);
        eventStore.append(eventInFuture);

        final EventFilter taskAddedType = EventFilter.newBuilder()
                                                     .setEventType(of(TaskAdded.class).value())
                                                     .build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(past)
                                                       .addFilter(taskAddedType)
                                                       .build();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Collection<Event> resultEvents = newConcurrentHashSet();
        eventStore.read(query, new ResponseObserver(resultEvents, done));
        assertDone(done);

        assertSize(1, resultEvents);
        final Event event = resultEvents.iterator()
                                        .next();
        assertEquals(eventInFuture, event);
    }

    @Test
    @DisplayName("read events by type")
    void readEventsByType() {
        final Timestamp now = getCurrentTime();

        final Event taskAdded1 = taskAdded(now);
        final Event projectCreated = projectCreated(now);
        final Event teasAdded2 = taskAdded(now);

        eventStore.append(taskAdded1);
        eventStore.append(projectCreated);
        eventStore.append(teasAdded2);

        final EventFilter taskAddedType = EventFilter.newBuilder()
                                                     .setEventType(of(TaskAdded.class).value())
                                                     .build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(taskAddedType)
                                                       .build();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Collection<Event> resultEvents = newConcurrentHashSet();
        eventStore.read(query, new ResponseObserver(resultEvents, done));
        assertDone(done);

        assertSize(2, resultEvents);
        assertContainsAll(resultEvents, taskAdded1, teasAdded2);
    }

    @Test
    @DisplayName("do nothing when appending empty iterable")
    void doNothingWhenAppendingEmptyIterable() {
        eventStore.appendAll(Collections.emptySet());
    }

    /**
     * Checks that the event storage is exposed to Beam-based catch-up code which is in the same
     * package but in a different module.
     */
    @Test
    @DisplayName("expose event repository to the package")
    void exposeEventRepository() {
        assertNotNull(eventStore.getStorage());
    }

    @Test
    @DisplayName("fail to store events of different tenants in a single operation")
    void notStoreEventsFromDifferentTenantsInSingleOperation() {
        final TenantId firstTenantId = TenantId.newBuilder()
                                               .setValue("abc")
                                               .build();
        final TenantId secondTenantId = TenantId.newBuilder()
                                                .setValue("xyz")
                                                .build();
        final ActorContext firstTenantActor = ActorContext.newBuilder()
                                                          .setTenantId(firstTenantId)
                                                          .build();
        final ActorContext secondTenantActor = ActorContext.newBuilder()
                                                           .setTenantId(secondTenantId)
                                                           .build();
        final CommandContext firstTenantCommand = CommandContext.newBuilder()
                                                                .setActorContext(firstTenantActor)
                                                                .build();
        final CommandContext secondTenantCommand = CommandContext.newBuilder()
                                                                 .setActorContext(secondTenantActor)
                                                                 .build();
        final EventContext firstTenantContext = EventContext.newBuilder()
                                                            .setCommandContext(firstTenantCommand)
                                                            .build();
        final EventContext secondTenantContext = EventContext.newBuilder()
                                                             .setCommandContext(secondTenantCommand)
                                                             .build();
        final Event firstTenantEvent = Event.newBuilder()
                                            .setContext(firstTenantContext)
                                            .build();
        final Event secondTenantEvent = Event.newBuilder()
                                             .setContext(secondTenantContext)
                                             .build();
        final Collection<Event> event = ImmutableSet.of(firstTenantEvent, secondTenantEvent);

        assertThrows(IllegalArgumentException.class, () -> eventStore.appendAll(event));
    }

    @Test
    @DisplayName("not store enrichment for EventContext")
    void notStoreEnrichmentForEventContext() {
        final Event event = projectCreated(Time.getCurrentTime());
        final Event enriched = event.toBuilder()
                                    .setContext(event.getContext()
                                                     .toBuilder()
                                                     .setEnrichment(withOneAttribute()))
                                    .build();
        eventStore.append(enriched);
        final MemoizingObserver<Event> observer = memoizingObserver();
        eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
        final EventContext context = observer.responses()
                                             .get(0)
                                             .getContext();
        assertTrue(isDefault(context.getEnrichment()));
    }

    @Test
    @DisplayName("not store enrichment for origin of RejectionContext type")
    void notStoreEnrichmentForOriginOfRejectionContextType() {
        final RejectionContext originContext = RejectionContext.newBuilder()
                                                               .setEnrichment(withOneAttribute())
                                                               .build();
        final Event event = projectCreated(Time.getCurrentTime());
        final Event enriched = event.toBuilder()
                                    .setContext(event.getContext()
                                                     .toBuilder()
                                                     .setRejectionContext(originContext))
                                    .build();
        eventStore.append(enriched);
        final MemoizingObserver<Event> observer = memoizingObserver();
        eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
        final RejectionContext loadedOriginContext = observer.responses()
                                                             .get(0)
                                                             .getContext()
                                                             .getRejectionContext();
        assertTrue(isDefault(loadedOriginContext.getEnrichment()));
    }

    @Test
    @DisplayName("not store enrichment for origin of EventContext type")
    void notStoreEnrichmentForOriginOfEventContextType() {
        final EventContext.Builder originContext = EventContext.newBuilder()
                                                               .setEnrichment(withOneAttribute());
        final Event event = projectCreated(Time.getCurrentTime());
        final Event enriched = event.toBuilder()
                                    .setContext(event.getContext()
                                                     .toBuilder()
                                                     .setEventContext(originContext))
                                    .build();
        eventStore.append(enriched);
        final MemoizingObserver<Event> observer = memoizingObserver();
        eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
        final EventContext loadedOriginContext = observer.responses()
                                                         .get(0)
                                                         .getContext()
                                                         .getEventContext();
        assertTrue(isDefault(loadedOriginContext.getEnrichment()));
    }

    /*
     * Test environment
     *********************/

    private static Event projectCreated(Timestamp when) {
        final ProjectCreated msg = Sample.messageOfType(ProjectCreated.class);
        return eventFactory.createEvent(msg, null, when);
    }

    private static Event taskAdded(Timestamp when) {
        final TaskAdded msg = Sample.messageOfType(TaskAdded.class);
        return eventFactory.createEvent(msg, null, when);
    }

    private static void assertDone(AtomicBoolean done) {
        if (!done.get()) {
            fail("Please use the MoreExecutors.directExecutor in EventStore for tests.");
        }
    }

    private static class ResponseObserver implements StreamObserver<Event> {

        private final Collection<Event> resultStorage;
        private final AtomicBoolean doneFlag;

        private ResponseObserver(Collection<Event> resultStorage, AtomicBoolean doneFlag) {
            this.resultStorage = resultStorage;
            this.doneFlag = doneFlag;
        }

        @Override
        public void onNext(Event value) {
            resultStorage.add(value);
        }

        @Override
        public void onError(Throwable t) {
            fail(t.getMessage());
        }

        @Override
        public void onCompleted() {
            doneFlag.getAndSet(true);
        }
    }
}
