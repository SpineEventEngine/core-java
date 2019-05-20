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

package io.spine.server.projection;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.entity.EntityHistoryIds;
import io.spine.server.event.DuplicateEventException;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.event.EventDispatchedToSubscriber;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.system.server.rejection.HistoryRejections.CannotDispatchEventTwice;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProjectionSystemEventWatcher should")
class ProjectionSystemEventWatcherTest {

    private static final TypeUrl REPOSITORY_TYPE = TypeUrl.of(Timestamp.class);
    private static final TypeUrl ANOTHER_TYPE = TypeUrl.of(Duration.class);

    private ProjectionRepository<?, ?, ?> repository;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectionRepository.class);
        when(repository.entityStateType()).thenReturn(REPOSITORY_TYPE);
    }

    @Test
    @DisplayName("be created successfully")
    void create() {
        ProjectionSystemEventWatcher<?> watcher = new ProjectionSystemEventWatcher<>(repository);
        assertNotNull(watcher);
    }

    @Test
    @DisplayName("not accept null repository")
    void notAcceptNullsInCtor() {
        new NullPointerTester()
                .testConstructors(ProjectionSystemEventWatcher.class, PACKAGE);
    }

    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() {
        ProjectionSystemEventWatcher<?> watcher = new ProjectionSystemEventWatcher<>(repository);
        new NullPointerTester()
                .testAllPublicInstanceMethods(watcher);
    }

    @Test
    @DisplayName("dispatch event")
    void event() {
        ProjectionSystemEventWatcher<?> watcher = new ProjectionSystemEventWatcher<>(repository);
        Event payload = GivenEvent.arbitrary();
        EventDispatchedToSubscriber systemEvent = EventDispatchedToSubscriber
                .newBuilder()
                .setPayload(payload)
                .setReceiver(historyId())
                .setWhenDispatched(currentTime())
                .build();
        watcher.on(systemEvent);

        verify(repository).dispatchNowTo(any(), eq(EventEnvelope.of(payload)));
    }

    @Test
    @DisplayName("warn repository with DuplicateEventException")
    void duplicateEvent() {
        ProjectionSystemEventWatcher<?> watcher = new ProjectionSystemEventWatcher<>(repository);
        Event payload = GivenEvent.arbitrary();
        CannotDispatchEventTwice rejection = CannotDispatchEventTwice
                .newBuilder()
                .setPayload(payload)
                .setReceiver(historyId())
                .setWhenDispatched(currentTime())
                .build();
        watcher.on(rejection);

        verify(repository).onError(
                eq(EventEnvelope.of(payload)),
                any(DuplicateEventException.class)
        );
    }

    @Nested
    @DisplayName("perform no action if type is wrong")
    class WrongType {

        private ProjectionSystemEventWatcher<?> watcher;

        @BeforeEach
        void setUp() {
            watcher = new ProjectionSystemEventWatcher<>(repository);
        }

        @Test
        @DisplayName("on event dispatching")
        void eventDispatched() {
            Event payload = GivenEvent.arbitrary();
            EventDispatchedToSubscriber systemEvent = EventDispatchedToSubscriber
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(wrongHistoryId())
                    .setWhenDispatched(currentTime())
                    .build();
            checkCannotDispatch(systemEvent, systemEvent.getReceiver());
        }

        @Test
        @DisplayName("on duplicate event rejection")
        void duplicateEvent() {
            Event payload = GivenEvent.arbitrary();
            CannotDispatchEventTwice rejection = CannotDispatchEventTwice
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(wrongHistoryId())
                    .setWhenDispatched(currentTime())
                    .build();
            checkCannotDispatch(rejection, rejection.getReceiver());
        }

        private void checkCannotDispatch(EventMessage eventMessage, EntityHistoryId producer) {
            TestEventFactory eventFactory =
                    TestEventFactory.newInstance(producer, ProjectionSystemEventWatcherTest.class);
            Event event = eventFactory.createEvent(eventMessage);
            EventEnvelope envelope = EventEnvelope.of(event);
            boolean canDispatch = watcher.canDispatch(envelope);
            assertFalse(canDispatch);
        }

        private EntityHistoryId wrongHistoryId() {
            return historyId().toBuilder()
                              .setTypeUrl(ANOTHER_TYPE.value())
                              .build();
        }
    }

    private static EntityHistoryId historyId() {
        return EntityHistoryIds.wrap(newUuid(), REPOSITORY_TYPE);
    }
}
