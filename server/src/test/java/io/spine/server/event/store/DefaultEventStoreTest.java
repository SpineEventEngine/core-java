/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Origin;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.given.EventStoreTestEnv.ResponseObserver;
import io.spine.server.type.given.GivenEvent;
import io.spine.test.event.TaskAdded;
import io.spine.testing.SlowTest;
import io.spine.testing.TestValues;
import io.spine.type.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.currentTime;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.event.given.EventStoreTestEnv.assertDone;
import static io.spine.server.event.given.EventStoreTestEnv.projectCreated;
import static io.spine.server.event.given.EventStoreTestEnv.taskAdded;
import static io.spine.testing.TestValues.random;
import static io.spine.testing.core.given.GivenEnrichment.withOneAttribute;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventStore should")
public class DefaultEventStoreTest {

    private BoundedContext context;
    private EventStore eventStore;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(64);
        context = BoundedContextBuilder.assumingTests().build();
        eventStore = context.eventBus().eventStore();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
        executor.shutdownNow();
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
        @DisplayName("time bounds and limit")
        void timeBoundsAndLimit() {
            Duration delta = seconds(222);
            Timestamp present = currentTime();
            Timestamp past = subtract(present, delta);
            Timestamp longPast = subtract(past, delta);
            Timestamp future = add(present, delta);

            Event eventInPast = projectCreated(past);
            Event eventInPresent = projectCreated(present);
            Event eventInFuture = projectCreated(future);

            eventStore.append(eventInPast);
            eventStore.append(eventInPresent);
            eventStore.append(eventInFuture);

            int expectedSize = 1;
            EventStreamQuery query = EventStreamQuery
                    .newBuilder()
                    .setAfter(longPast)
                    .setBefore(future)
                    .setLimit(limitOf(expectedSize))
                    .build();
            AtomicBoolean done = new AtomicBoolean(false);
            ResponseObserver observer = new ResponseObserver(done);
            eventStore.read(query, observer);
            Collection<Event> resultEvents = observer.getEvents();

            assertDone(done);
            assertThat(resultEvents).hasSize(expectedSize);
            Event actualEvent = resultEvents.iterator()
                                            .next();
            assertEquals(eventInPast, actualEvent);
        }

        @Test
        @DisplayName("type")
        void type() {
            Timestamp now = currentTime();

            Event taskAdded1 = taskAdded(now);
            Event projectCreated = projectCreated(now);
            Event taskAdded2 = taskAdded(now);

            eventStore.append(taskAdded1);
            eventStore.append(projectCreated);
            eventStore.append(taskAdded2);

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
            assertResultEvents.containsExactly(taskAdded1, taskAdded2);
        }

        @Test
        @DisplayName("type and limit")
        void typeAndLimit() {
            Timestamp now = currentTime();
            Timestamp future = add(now, seconds(1));

            Event taskAdded1 = taskAdded(now);
            Event projectCreated = projectCreated(now);
            Event taskAdded2 = taskAdded(future);

            eventStore.append(taskAdded1);
            eventStore.append(projectCreated);
            eventStore.append(taskAdded2);

            EventFilter taskAddedType = EventFilter
                    .newBuilder()
                    .setEventType(TypeName.of(TaskAdded.class)
                                          .value())
                    .build();
            int expectedSize = 1;
            EventStreamQuery query = EventStreamQuery
                    .newBuilder()
                    .addFilter(taskAddedType)
                    .setLimit(limitOf(expectedSize))
                    .build();
            AtomicBoolean done = new AtomicBoolean(false);
            ResponseObserver observer = new ResponseObserver(done);
            eventStore.read(query, observer);
            assertDone(done);

            IterableSubject assertResultEvents = assertThat(observer.getEvents());
            assertResultEvents.hasSize(1);
            assertResultEvents.containsExactly(taskAdded1);
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

        @Test
        @DisplayName("time bounds, type and limit")
        void timeBoundsTypeAndFuture() {
            Duration delta = seconds(111);
            Timestamp present = currentTime();
            Timestamp past = subtract(present, delta);
            Timestamp future = add(present, delta);
            Timestamp distantFuture = add(future, delta);

            Event eventInPast = taskAdded(past);
            Event eventInPresent = projectCreated(present);
            Event eventInFuture = taskAdded(future);
            Event eventInDistantFuture = taskAdded(distantFuture);

            eventStore.append(eventInPast);
            eventStore.append(eventInPresent);
            eventStore.append(eventInFuture);
            eventStore.append(eventInDistantFuture);

            EventFilter taskAddedType = EventFilter
                    .newBuilder()
                    .setEventType(TypeName.of(TaskAdded.class)
                                          .value())
                    .build();
            EventStreamQuery query = EventStreamQuery
                    .newBuilder()
                    .setAfter(past)
                    .addFilter(taskAddedType)
                    .setLimit(limitOf(1))
                    .build();
            AtomicBoolean done = new AtomicBoolean(false);
            ResponseObserver observer = new ResponseObserver(done);
            eventStore.read(query, observer);
            assertDone(done);

            IterableSubject assertResultEvents = assertThat(observer.getEvents());
            assertResultEvents.hasSize(1);
            assertResultEvents.containsExactly(eventInFuture);
        }

        private EventStreamQuery.Limit limitOf(int value) {
            return EventStreamQuery.Limit
                    .newBuilder()
                    .setValue(value)
                    .vBuild();
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
                .buildPartial();
        TenantId secondTenantId = TenantId
                .newBuilder()
                .setValue("xyz")
                .buildPartial();
        ActorContext firstTenantActor = ActorContext
                .newBuilder()
                .setTenantId(firstTenantId)
                .buildPartial();
        ActorContext secondTenantActor = ActorContext
                .newBuilder()
                .setTenantId(secondTenantId)
                .buildPartial();
        Origin firstTenantOrigin = Origin
                .newBuilder()
                .setActorContext(firstTenantActor)
                .buildPartial();
        Origin secondTenantOrigin = Origin
                .newBuilder()
                .setActorContext(secondTenantActor)
                .buildPartial();
        EventContext firstTenantContext = EventContext
                .newBuilder()
                .setPastMessage(firstTenantOrigin)
                .buildPartial();
        EventContext secondTenantContext = EventContext
                .newBuilder()
                .setPastMessage(secondTenantOrigin)
                .buildPartial();
        Event firstTenantEvent = Event
                .newBuilder()
                .setContext(firstTenantContext)
                .buildPartial();
        Event secondTenantEvent = Event
                .newBuilder()
                .setContext(secondTenantContext)
                .buildPartial();
        Collection<Event> event = ImmutableSet.of(firstTenantEvent, secondTenantEvent);

        assertThrows(IllegalArgumentException.class, () -> eventStore.appendAll(event));
    }

    @SlowTest
    @Test
    @DisplayName("be able to store events in parallel")
    void storeInParallel() {
        int eventCount = 10_000;
        CompletableFuture<?>[] futures =
                Stream.generate(GivenEvent::arbitrary)
                      .limit(eventCount)
                      .map(event -> runAsync(() -> waitAndStore(event), executor))
                      .toArray(CompletableFuture[]::new);
        CompletableFuture<Void> allFutures = allOf(futures);
        allFutures.join();
        MemoizingObserver<Event> observer = memoizingObserver();
        eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
        assertThat(observer.isCompleted())
                .isTrue();
        assertThat(observer.responses())
                .hasSize(eventCount);
    }

    private void waitAndStore(Event event) {
        sleepUninterruptibly(ofMillis(random(20, 40)));
        eventStore.append(event);
    }

    @Nested
    @DisplayName("not store enrichment for")
    class NotStoreEnrichmentFor {

        @Test
        @DisplayName("EventContext")
        void eventContext() {
            Event event = projectCreated(Time.currentTime());
            Event enriched = event.toBuilder()
                                  .setContext(event.context()
                                                   .toBuilder()
                                                   .setEnrichment(withOneAttribute()))
                                  .build();
            eventStore.append(enriched);
            MemoizingObserver<Event> observer = memoizingObserver();
            eventStore.read(EventStreamQuery.getDefaultInstance(), observer);
            EventContext context = observer.responses()
                                           .get(0)
                                           .context();
            assertTrue(isDefault(context.getEnrichment()));
        }

        @SuppressWarnings("deprecation")
            // Enrichment cleanup still checks for the deprecated fields, so keep the test.
        @Test
        @DisplayName("origin of EventContext type")
        void eventContextOrigin() {
            Event event = projectCreated(Time.currentTime());
            Origin pastMessage = event.context()
                                      .getPastMessage();
            EventContext originContext =
                    EventContext.newBuilder()
                                .setEnrichment(withOneAttribute())
                                .setPastMessage(pastMessage)
                                .setTimestamp(event.context()
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
                                                       .context()
                                                       .getEventContext();
            assertTrue(isDefault(loadedOriginContext.getEnrichment()));
        }
    }
}
