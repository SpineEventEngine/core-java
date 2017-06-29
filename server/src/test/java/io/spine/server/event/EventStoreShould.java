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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.TenantId;
import io.spine.core.given.GivenCommandContext;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.TaskAdded;
import io.spine.testdata.Sample;
import io.spine.time.Durations2;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.test.Verify.assertSize;
import static io.spine.time.Time.getCurrentTime;
import static io.spine.type.TypeName.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Dmytro Dashenkov
 */
public class EventStoreShould {

    private static TestEventFactory eventFactory = null;

    private EventStore eventStore;

    protected EventStore creteStore() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(false)
                                                .build();
        return EventStore.newBuilder()
                         .setStorageFactory(bc.getStorageFactory())
                         .setStreamExecutor(MoreExecutors.directExecutor())
                         .withDefaultLogger()
                         .build();
    }

    @BeforeClass
    public static void prepare() {
        final CommandContext context = GivenCommandContext.withRandomUser();
        eventFactory = TestEventFactory.newInstance(EventStoreShould.class, context);
    }

    @Before
    public void setUp() {
        eventStore = creteStore();
    }

    @Test
    public void read_events_by_time_bounds() {
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
    public void read_events_by_time_and_type() {
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
    public void read_events_by_type() {
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
    public void do_nothing_when_appending_empty_iterable() {
        eventStore.appendAll(Collections.<Event>emptySet());
    }

    /**
     * Checks that the event storage is exposed to Beam-based catch-up code which is in the same
     * package but in a different module.
     */
    @Test
    public void expose_event_repository_to_the_package() {
        assertNotNull(eventStore.getStorage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_store_events_of_different_tenants_in_a_single_operation() {
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
        eventStore.appendAll(event);
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
