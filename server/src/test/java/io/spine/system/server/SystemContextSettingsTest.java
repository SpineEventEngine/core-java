/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.system.server;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.given.entity.HistoryEventWatcher;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.option.EntityOption.Kind.ENTITY;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System Bounded Context should")
class SystemContextSettingsTest {

    private static final TestEventFactory events =
            TestEventFactory.newInstance(SystemContextSettingsTest.class);

    @Test
    @DisplayName("not store events by default")
    void notStoreEvents() {
        BoundedContext domain = BoundedContextBuilder
                .assumingTests()
                .build();
        BoundedContext system = systemOf(domain);
        Event event = createEvent();
        MemoizingObserver<Event> observer = postSystemEvent(system.eventBus(), event);
        assertTrue(observer.isCompleted());
        assertThat(observer.responses()).isEmpty();
    }

    @Test
    @DisplayName("store events if required")
    void storeEvents() {
        BoundedContextBuilder contextBuilder = BoundedContextBuilder.assumingTests();
        contextBuilder.systemSettings()
                      .persistEvents();
        BoundedContext domain = contextBuilder.build();
        BoundedContext system = systemOf(domain);
        Event event = createEvent();
        MemoizingObserver<Event> observer = postSystemEvent(system.eventBus(), event);
        assertTrue(observer.isCompleted());
        assertThat(observer.responses()).containsExactly(event);
    }

    @Test
    @DisplayName("not store domain commands")
    void notStoreDomainCommands() {
        BoundedContext domain = BoundedContextBuilder
                .assumingTests()
                .build();
        BoundedContext system = systemOf(domain);
        assertFalse(system.hasEntitiesWithState(CommandLog.class));
    }

    @Test
    @DisplayName("store domain commands if required")
    void storeDomainCommands() {
        BoundedContextBuilder contextBuilder = BoundedContextBuilder.assumingTests();
        contextBuilder.systemSettings()
                      .enableCommandLog();
        BoundedContext domain = contextBuilder.build();
        BoundedContext system = systemOf(domain);
        assertTrue(system.hasEntitiesWithState(CommandLog.class));
    }

    @Test
    @DisplayName("post system events in parallel")
    void asyncEvents() {
        BoundedContextBuilder contextBuilder = BoundedContextBuilder.assumingTests();
        contextBuilder.systemSettings()
                      .enableParallelPosting();
        BoundedContext domain = contextBuilder.build();
        BoundedContext system = systemOf(domain);
        HistoryEventWatcher watcher = new HistoryEventWatcher();
        system.eventBus().register(watcher);
        MessageId messageId = MessageId.newBuilder()
                                       .setTypeUrl(TypeUrl.of(Empty.class).value())
                                       .setId(Identifier.pack(newUuid()))
                                       .build();
        EntityCreated event = EntityCreated
                .newBuilder()
                .setEntity(messageId)
                .setKind(ENTITY)
                .build();
        domain.systemClient()
              .writeSide()
              .postEvent(event);
        sleepUninterruptibly(ofSeconds(1));
        watcher.assertReceivedEvent(EntityCreated.class);
    }

    private static MemoizingObserver<Event> postSystemEvent(EventBus systemBus, Event event) {
        systemBus.post(event);
        EventFilter filter = EventFilter
                .newBuilder()
                .setEventType(event.enclosedTypeUrl()
                                   .toTypeName().value())
                .vBuild();
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .addFilter(filter)
                .vBuild();
        MemoizingObserver<Event> observer = memoizingObserver();
        systemBus.eventStore()
                 .read(query, observer);
        return observer;
    }

    private static Event createEvent() {
        EntityStateChanged eventMessage = EntityStateChanged
                .newBuilder()
                .setEntity(MessageId.newBuilder()
                                    .setId(Identifier.pack(42))
                                    .setTypeUrl(TypeUrl.of(EmptyEntityState.class)
                                                       .value()))
                .setOldState(pack(StringValue.of("0")))
                .setNewState(pack(StringValue.of("42")))
                .addSignalId(MessageId.newBuilder()
                                      .setId(Identifier.pack(CommandId.generate()))
                                      .setTypeUrl(TypeUrl.of(EntityStateChanged.class)
                                                         .value()))
                .vBuild();
        Event event = events.createEvent(eventMessage);
        return event;
    }
}
