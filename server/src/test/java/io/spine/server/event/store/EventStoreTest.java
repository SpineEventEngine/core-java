/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.event.store;

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.protobuf.AnyPacker;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.given.EventStoreTestEnv.ResponseObserver;
import io.spine.test.event.TaskAdded;
import io.spine.testing.TestValues;
import io.spine.type.TypeName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.currentTime;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.server.event.given.EventStoreTestEnv.assertDone;
import static io.spine.server.event.given.EventStoreTestEnv.eventStore;
import static io.spine.server.event.given.EventStoreTestEnv.initEventFactory;
import static io.spine.server.event.given.EventStoreTestEnv.projectCreated;
import static io.spine.server.event.given.EventStoreTestEnv.taskAdded;
import static io.spine.testing.core.given.GivenEnrichment.withOneAttribute;
import static io.spine.validate.Validate.isDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventStore should")
public class EventStoreTest {

    private EventStore eventStore;

    @BeforeAll
    static void prepare() {
        initEventFactory();
    }

    @BeforeEach
    void setUp() {
        eventStore = eventStore();
    }

    @Nested
    @DisplayName("read events by")
    class ReadEventsBy {

        @Test
        @DisplayName("time bounds")
        void timeBounds() {
            Duration delta = seconds(111);
            Timestamp present = currentTime();
            Timestamp past = subtract(present, delta);
            Timestamp future = add(present, delta);

            Event eventInPast = projectCreated(past);
            Event eventInPresent = projectCreated(present);
            Event eventInFuture = projectCreated(future);

            eventStore.append(eventInPast);
            eventStore.append(eventInPresent);
            eventStore.append(eventInFuture);

            EventStreamQuery query = EventStreamQuery
                    .newBuilder()
                    .setAfter(past)
                    .setBefore(future)
                    .build();
            AtomicBoolean done = new AtomicBoolean(false);
            ResponseObserver observer = new ResponseObserver(done);
            eventStore.read(query, observer);
            Collection<Event> resultEvents = observer.getEvents();

            assertDone(done);
            assertThat(resultEvents).hasSize(1);
            Event event = resultEvents.iterator()
                                      .next();
            assertEquals(eventInPresent, event);
        }

        @Test
        @DisplayName("type")
        void type() {
            Timestamp now = currentTime();

            Event taskAdded1 = taskAdded(now);
            Event projectCreated = projectCreated(now);
            Event teasAdded2 = taskAdded(now);

            eventStore.append(taskAdded1);
            eventStore.append(projectCreated);
            eventStore.append(teasAdded2);

            EventFilter taskAddedType = EventFilter
                    .newBuilder()
                    .setEventType(TypeName.of(TaskAdded.class)
                                          .value())
                    .build();
            EventStreamQuery query = EventStreamQuery
                    .newBuilder()
                    .addFilter(taskAddedType)
                    .build();
            AtomicBoolean done = new AtomicBoolean(false);
            ResponseObserver observer = new ResponseObserver(done);
            eventStore.read(query, observer);
            assertDone(done);

            IterableSubject assertResultEvents = assertThat(observer.getEvents());
            assertResultEvents.hasSize(2);
            assertResultEvents.containsExactly(taskAdded1, teasAdded2);
        }

        @Test
        @DisplayName("time bounds and type")
        void timeBoundsAndType() {
            Duration delta = seconds(111);
            Timestamp present = currentTime();
            Timestamp past = subtract(present, delta);
            Timestamp future = add(present, delta);

            Event eventInPast = taskAdded(past);
            Event eventInPresent = projectCreated(present);
            Event eventInFuture = taskAdded(future);

            eventStore.append(eventInPast);
            eventStore.append(eventInPresent);
            eventStore.append(eventInFuture);

            EventFilter taskAddedType = EventFilter
                    .newBuilder()
                    .setEventType(TypeName.of(TaskAdded.class)
                                          .value())
                    .build();
            EventStreamQuery query = EventStreamQuery
                    .newBuilder()
                    .setAfter(past)
                    .addFilter(taskAddedType)
                    .build();
            AtomicBoolean done = new AtomicBoolean(false);
            ResponseObserver observer = new ResponseObserver(done);
            eventStore.read(query, observer);
            assertDone(done);

            IterableSubject assertResultEvents = assertThat(observer.getEvents());
            assertResultEvents.hasSize(1);
            assertResultEvents.containsExactly(eventInFuture);
        }
    }

    @Test
    @DisplayName("do nothing when appending empty iterable")
    void processEmptyIterable() {
        eventStore.appendAll(Collections.emptySet());
    }

    @Test
    @DisplayName("fail to store events of different tenants in single operation")
    void rejectEventsFromDifferentTenants() {
        TenantId firstTenantId = TenantId
                .newBuilder()
                .setValue("abc")
                .build();
        TenantId secondTenantId = TenantId
                .newBuilder()
                .setValue("xyz")
                .build();
        ActorContext firstTenantActor = ActorContext
                .newBuilder()
                .setTenantId(firstTenantId)
                .build();
        ActorContext secondTenantActor = ActorContext
                .newBuilder()
                .setTenantId(secondTenantId)
                .build();
        CommandContext firstTenantCommand = CommandContext
                .newBuilder()
                .setActorContext(firstTenantActor)
                .build();
        CommandContext secondTenantCommand = CommandContext
                .newBuilder()
                .setActorContext(secondTenantActor)
                .build();
        EventContext firstTenantContext = EventContext
                .newBuilder()
                .setCommandContext(firstTenantCommand)
                .build();
        EventContext secondTenantContext = EventContext
                .newBuilder()
                .setCommandContext(secondTenantCommand)
                .build();
        Event firstTenantEvent = Event
                .newBuilder()
                .setContext(firstTenantContext)
                .build();
        Event secondTenantEvent = Event
                .newBuilder()
                .setContext(secondTenantContext)
                .build();
        Collection<Event> event = ImmutableSet.of(firstTenantEvent, secondTenantEvent);

        assertThrows(IllegalArgumentException.class, () -> eventStore.appendAll(event));
    }

    @Nested
    @DisplayName("not store enrichment for")
    class NotStoreEnrichmentFor {

        @Test
        @DisplayName("EventContext")
        void eventContext() {
            Event event = projectCreated(Time.currentTime());
            Event enriched = event.toBuilder()
                                  .setContext(event.getContext()
                                                   .toBuilder()
                                                   .setEnrichment(withOneAttribute()))
                                  .build();
            eventStore.append(enriched);
            MemoizingObserver<Event> observer = memoizingObserver();
            eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
            EventContext context = observer.responses()
                                           .get(0)
                                           .getContext();
            assertTrue(isDefault(context.getEnrichment()));
        }

        @Test
        @DisplayName("origin of EventContext type")
        void eventContextOrigin() {
            Event event = projectCreated(Time.currentTime());
            CommandContext commandContext = event.getContext()
                                                 .getCommandContext();
            EventContext originContext =
                    EventContext.vBuilder()
                                .setEnrichment(withOneAttribute())
                                .setCommandContext(commandContext)
                                .setTimestamp(event.getContext()
                                                   .getTimestamp())
                                .setProducerId(AnyPacker.pack(TestValues.newUuidValue()))
                                .build();
            Event enriched = event.toBuilder()
                                  .setContext(event.getContext()
                                                   .toBuilder()
                                                   .setEventContext(originContext))
                                  .build();
            eventStore.append(enriched);
            MemoizingObserver<Event> observer = memoizingObserver();
            eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
            EventContext loadedOriginContext = observer.responses()
                                                       .get(0)
                                                       .getContext()
                                                       .getEventContext();
            assertTrue(isDefault(loadedOriginContext.getEnrichment()));
        }
    }
}
