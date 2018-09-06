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

package io.spine.system.server;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.server.BoundedContext;
import io.spine.test.system.server.Photo;
import io.spine.test.system.server.PhotoId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.ShardingReset;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Streams.stream;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpackFunc;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.mirror.RepositoryTestEnv.givenPhotos;
import static io.spine.system.server.given.mirror.RepositoryTestEnv.historyIdOf;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dmytro Dashenkov
 */
@ExtendWith(ShardingReset.class)
@DisplayName("MirrorRepository should")
class MirrorRepositoryTest {

    private static final TestEventFactory events = newInstance(MirrorRepositoryTest.class);

    private MirrorRepository repository;
    private QueryFactory queries;

    @BeforeEach
    void setUp() {
        BoundedContext domainContext = BoundedContext.newBuilder().build();
        BoundedContext systemContext = systemOf(domainContext);
        repository = (MirrorRepository) systemContext
                .findRepository(Mirror.class)
                .orElseGet(() -> fail("MirrorRepository must be registered."));
        queries = TestActorRequestFactory.newInstance(MirrorRepositoryTest.class).query();
    }

    @Nested
    @DisplayName("on an aggregate query")
    class ExecuteQueries {

        @Nested
        @DisplayName("for a known type")
        class Known {

            private Map<EntityHistoryId, Photo> givenPhotos;

            @BeforeEach
            void setUp() {
                givenPhotos = givenPhotos();
                prepareAggregates(givenPhotos);
            }

            @Test
            @DisplayName("find all instances")
            void includeAll() {
                Query query = queries.all(Photo.class);
                List<? extends Message> readMessages = execute(query);
                assertThat(readMessages, containsInAnyOrder(givenPhotos.values().toArray()));
            }

            @Test
            @DisplayName("find an instance by ID")
            void byId() {
                Photo target = onePhoto();
                checkRead(target);
            }

            @Test
            @DisplayName("find an archived aggregate by ID")
            void archived() {
                Photo target = onePhoto();
                archive(target);
                PhotoId targetId = target.getId();
                Query query = queries.select(Photo.class)
                                     .byId(targetId)
                                     .where(eq(archived.name(), true))
                                     .build();
                checkOneRead(query, target);
            }

            @Test
            @DisplayName("find a deleted aggregate by ID")
            void deleted() {
                Photo target = onePhoto();
                delete(target);
                PhotoId targetId = target.getId();
                Query query = queries.select(Photo.class)
                                     .byId(targetId)
                                     .where(eq(deleted.name(), true))
                                     .build();
                checkOneRead(query, target);
            }

            private void checkRead(Photo target) {
                PhotoId targetId = target.getId();
                Query query = queries.byIds(Photo.class, of(targetId));
                checkOneRead(query, target);
            }

            private void checkOneRead(Query query, Photo expected) {
                List<? extends Message> readMessages = execute(query);
                assertEquals(1, readMessages.size());
                assertEquals(expected, readMessages.get(0));
            }

            private Photo onePhoto() {
                Photo target = givenPhotos.values()
                                          .stream()
                                          .findAny()
                                          .orElseGet(() -> fail("No test data."));
                return target;
            }
        }

        @Test
        @DisplayName("for an unknown type")
        void unknown() {
            Query query = queries.all(Timestamp.class);

            Iterator<Any> result = repository.execute(query);
            assertFalse(result.hasNext());
        }
    }

    private void prepareAggregates(Map<EntityHistoryId, ? extends Message> aggregateStates) {
        aggregateStates.entrySet()
                       .stream()
                       .map(entry -> entityStateChanged(entry.getKey(), entry.getValue()))
                       .forEach(this::dispatchEvent);
    }

    private void archive(Photo photo) {
        EntityHistoryId historyId = historyIdOf(photo);
        EntityArchived archivedEvent = EntityArchived
                .newBuilder()
                .setId(historyId)
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .build();
        Event event = events.createEvent(archivedEvent);
        dispatchEvent(event);
    }

    private void delete(Photo photo) {
        EntityHistoryId historyId = historyIdOf(photo);
        EntityDeleted deletedEvent = EntityDeleted
                .newBuilder()
                .setId(historyId)
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .build();
        Event event = events.createEvent(deletedEvent);
        dispatchEvent(event);
    }


    @SuppressWarnings("CheckReturnValue")
    private void dispatchEvent(Event event) {
        EventEnvelope envelope = EventEnvelope.of(event);
        repository.dispatch(envelope);
    }

    private List<? extends Message> execute(Query query) {
        Iterator<Any> result = repository.execute(query);
        List<? extends Message> readMessages = stream(result)
                .map(unpackFunc())
                .collect(toList());
        return readMessages;
    }

    private static Event entityStateChanged(EntityHistoryId historyId, Message state) {
        EntityStateChanged eventMessage = EntityStateChanged
                .newBuilder()
                .setId(historyId)
                .setNewState(pack(state))
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .build();
        Event event = events.createEvent(eventMessage);
        return event;
    }

    private static DispatchedMessageId cause() {
        EventId causeOfChange = EventId
                .newBuilder()
                .setValue("For tests")
                .build();
        DispatchedMessageId messageId = DispatchedMessageId
                .newBuilder()
                .setEventId(causeOfChange)
                .build();
        return messageId;
    }
}
