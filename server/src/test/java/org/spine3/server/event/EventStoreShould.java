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

package org.spine3.server.event;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.test.TestEventFactory;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.TaskAdded;
import org.spine3.testdata.Sample;
import org.spine3.time.Durations2;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.spine3.test.Verify.assertContainsAll;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.time.Time.getCurrentTime;
import static org.spine3.type.TypeName.of;

/**
 * @author Dmytro Dashenkov
 */
public abstract class EventStoreShould {

    private static TestEventFactory eventFactory = null;

    private EventStore eventStore;

    protected abstract EventStore creteStore();

    @BeforeClass
    public static void prepare() {
        final CommandContext context = createCommandContext();
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
        if (!done.get()) {
            fail("Please use the MoreExecutors.directExecutor in EventStore for tests.");
        }

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
        if (!done.get()) {
            fail("Please use the MoreExecutors.directExecutor in EventStore for tests.");
        }

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
        if (!done.get()) {
            fail("Please use the MoreExecutors.directExecutor in EventStore for tests.");
        }

        assertSize(2, resultEvents);
        assertContainsAll(resultEvents, taskAdded1, teasAdded2);
    }

    private static Event projectCreated(Timestamp when) {
        final ProjectCreated msg = Sample.messageOfType(ProjectCreated.class);
        return eventFactory.createEvent(msg, null, when);
    }

    private static Event taskAdded(Timestamp when) {
        final TaskAdded msg = Sample.messageOfType(TaskAdded.class);
        return eventFactory.createEvent(msg, null, when);
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
