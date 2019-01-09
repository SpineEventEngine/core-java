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

package io.spine.server.procman;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.client.EntityId;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.event.DuplicateEventException;
import io.spine.server.procman.given.delivery.GivenMessage;
import io.spine.system.server.CommandDispatchedToHandler;
import io.spine.system.server.CommandDispatchedToHandlerVBuilder;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EventDispatchedToReactor;
import io.spine.system.server.EventDispatchedToReactorVBuilder;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.system.server.HistoryRejections.CannotDispatchCommandTwice;
import static io.spine.system.server.HistoryRejections.CannotDispatchEventTwice;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PmSystemEventWatcher should")
class PmSystemEventWatcherTest {

    private static final TypeUrl REPOSITORY_TYPE = TypeUrl.of(Timestamp.class);
    private static final TypeUrl ANOTHER_TYPE = TypeUrl.of(Duration.class);

    private ProcessManagerRepository<?, ?, ?> repository;
    private PmSystemEventWatcher<?> watcher;

    @BeforeEach
    void setUp() {
        repository = mock(ProcessManagerRepository.class);
        when(repository.getEntityStateType()).thenReturn(REPOSITORY_TYPE);

        watcher = new PmSystemEventWatcher<>(repository);
    }

    @Test
    @DisplayName("be created successfully")
    void create() {
        PmSystemEventWatcher<?> watcher = new PmSystemEventWatcher<>(repository);
        assertNotNull(watcher);
    }

    @Test
    @DisplayName("not accept null repository")
    void notAcceptNullsInCtor() {
        new NullPointerTester()
                .testConstructors(PmSystemEventWatcher.class, PACKAGE);
    }

    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() {
        PmSystemEventWatcher<?> watcher = new PmSystemEventWatcher<>(repository);
        new NullPointerTester()
                .testAllPublicInstanceMethods(watcher);
    }

    @Nested
    @DisplayName("on a 'dispatched' system event, dispatch")
    class PositiveOutcome {

        @Test
        @DisplayName("event")
        void event() {
            Event payload = GivenMessage.projectStarted();
            EventDispatchedToReactor systemEvent = EventDispatchedToReactorVBuilder
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(historyId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            watcher.on(systemEvent);

            verify(repository).dispatchNowTo(any(), eq(EventEnvelope.of(payload)));
        }

        @Test
        @DisplayName("command")
        void command() {
            Command payload = GivenMessage.createProject();
            CommandDispatchedToHandler systemEvent = CommandDispatchedToHandlerVBuilder
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(historyId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            watcher.on(systemEvent);

            verify(repository).dispatchNowTo(any(), eq(CommandEnvelope.of(payload)));
        }
    }

    @Nested
    @DisplayName("on a system rejection, warn repository with")
    class NegativeOutcome {

        @Test
        @DisplayName("DuplicateEventException")
        void event() {
            Event payload = GivenMessage.projectStarted();
            CannotDispatchEventTwice rejection = CannotDispatchEventTwice
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(historyId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            watcher.on(rejection);

            verify(repository).onError(
                    eq(EventEnvelope.of(payload)),
                    any(DuplicateEventException.class)
            );
        }

        @Test
        @DisplayName("DuplicateCommandException")
        void command() {
            Command payload = GivenMessage.createProject();
            CannotDispatchCommandTwice rejection = CannotDispatchCommandTwice
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(historyId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            watcher.on(rejection);

            verify(repository).onError(
                    eq(CommandEnvelope.of(payload)),
                    any(DuplicateCommandException.class)
            );
        }
    }

    @Nested
    @DisplayName("perform no action if type is wrong")
    class WrongType {

        @SuppressWarnings("unchecked") // `clearInvocations(...)` expects a vararg.
        @BeforeEach
        void setUp() {
            clearInvocations(repository);
        }

        @Test
        @DisplayName("on event dispatching")
        void eventDispatched() {
            Event payload = GivenMessage.projectStarted();
            EventDispatchedToReactor systemEvent = EventDispatchedToReactorVBuilder
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(wrongHistoryId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            checkCannotDispatch(systemEvent, systemEvent.getReceiver());
        }

        @Test
        @DisplayName("on command dispatching")
        void commandDispatched() {
            Command payload = GivenMessage.createProject();
            CommandDispatchedToHandler systemEvent = CommandDispatchedToHandlerVBuilder
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(wrongHistoryId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            checkCannotDispatch(systemEvent, systemEvent.getReceiver());
        }

        @Test
        @DisplayName("on duplicate event rejection")
        void duplicateEvent() {
            Event payload = GivenMessage.projectStarted();
            CannotDispatchEventTwice rejection = CannotDispatchEventTwice
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(wrongHistoryId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            checkCannotDispatch(rejection, rejection.getReceiver());
        }

        @Test
        @DisplayName("on duplicate command rejection")
        void command() {
            Command payload = GivenMessage.createProject();
            CannotDispatchCommandTwice rejection = CannotDispatchCommandTwice
                    .newBuilder()
                    .setPayload(payload)
                    .setReceiver(wrongHistoryId())
                    .setWhenDispatched(getCurrentTime())
                    .build();
            checkCannotDispatch(rejection, rejection.getReceiver());
        }

        private EntityHistoryId wrongHistoryId() {
            return historyId().toBuilder()
                              .setTypeUrl(ANOTHER_TYPE.value())
                              .build();
        }

        private void checkCannotDispatch(EventMessage eventMessage, EntityHistoryId producer) {
            TestEventFactory eventFactory =
                    TestEventFactory.newInstance(producer, PmSystemEventWatcher.class);
            Event event = eventFactory.createEvent(eventMessage);
            EventEnvelope envelope = EventEnvelope.of(event);
            boolean canDispatch = watcher.canDispatch(envelope);
            assertFalse(canDispatch);
        }
    }

    private static EntityHistoryId historyId() {
        Any id = pack(newUuid());
        EntityId entityId = EntityId
                .newBuilder()
                .setId(id)
                .build();
        return EntityHistoryId
                .newBuilder()
                .setTypeUrl(REPOSITORY_TYPE.value())
                .setEntityId(entityId)
                .build();
    }
}
