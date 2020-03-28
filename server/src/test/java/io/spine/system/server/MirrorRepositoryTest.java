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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.system.server.MRIncompleteAudio;
import io.spine.test.system.server.MRLocalizedVideo;
import io.spine.test.system.server.MRPhoto;
import io.spine.test.system.server.MRPhotoId;
import io.spine.test.system.server.MRVideo;
import io.spine.test.system.server.MRVideoId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.MuteLogging;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.client.Filters.eq;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpackFunc;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.archived;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.cause;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.deleted;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.entityStateChanged;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.event;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.givenPhotos;
import static io.spine.system.server.given.mirror.MirrorRepositoryTestEnv.newPhotosRepository;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("MirrorRepository should")
@Disabled
class MirrorRepositoryTest {

    private MirrorRepository repository;
    private QueryFactory queries;

    private Map<MessageId, MRPhoto> givenPhotos;

    @BeforeEach
    void setUp() {
        BoundedContext domainContext = BoundedContextBuilder.assumingTests(false)
                                                            .build();
        BoundedContext systemContext = systemOf(domainContext);
        domainContext.register(newPhotosRepository());

        repository = (MirrorRepository) systemContext
                .findRepository(Mirror.class)
                .orElseGet(() -> fail("MirrorRepository must be registered."));
        queries = new TestActorRequestFactory(MirrorRepositoryTest.class).query();

        givenPhotos = givenPhotos();
        prepareAggregates(givenPhotos);
    }

    @Nested
    @DisplayName("on an aggregate query")
    class ExecuteQueries {

        @Nested
        @DisplayName("for a known type")
        class Known {

            @Test
            @DisplayName("find all instances")
            void includeAll() {
                Query query = queries.all(MRPhoto.class);
                List<? extends EntityState> readMessages = execute(query);
                assertThat(readMessages, containsInAnyOrder(givenPhotos.values()
                                                                       .toArray()));
            }

            @Test
            @DisplayName("find an instance by ID")
            void byId() {
                MRPhoto target = onePhoto();
                readAndCheck(target);
            }

            @Test
            @DisplayName("find an archived aggregate by ID")
            void archivedInstance() {
                MRPhoto target = onePhoto();
                archive(target);
                MRPhotoId targetId = target.getId();
                Query query = queries.select(MRPhoto.class)
                                     .byId(targetId)
                                     .where(eq(archived.name(), true))
                                     .build();
                checkRead(query, target);
            }

            @Test
            @DisplayName("find a deleted aggregate by ID")
            void deletedInstance() {
                MRPhoto target = onePhoto();
                delete(target);
                MRPhotoId targetId = target.getId();
                Query query = queries.select(MRPhoto.class)
                                     .byId(targetId)
                                     .where(eq(deleted.name(), true))
                                     .build();
                checkRead(query, target);
            }

            @Test
            @DisplayName("find all archived instances")
            void allArchived() {
                MRPhoto firstPhoto = onePhoto();
                MRPhoto secondPhoto = anotherPhoto();
                archive(firstPhoto);
                archive(secondPhoto);

                Query query = queries.select(MRPhoto.class)
                                     .where(eq(archived.name(), true),
                                            eq(deleted.name(), false))
                                     .build();
                checkRead(query, firstPhoto, secondPhoto);
            }

            @Test
            @DisplayName("find all deleted instances")
            void allDeleted() {
                MRPhoto firstPhoto = onePhoto();
                MRPhoto secondPhoto = anotherPhoto();
                delete(firstPhoto);
                delete(secondPhoto);

                Query query = queries.select(MRPhoto.class)
                                     .where(eq(deleted.name(), true),
                                            eq(archived.name(), false))
                                     .build();
                checkRead(query, firstPhoto, secondPhoto);
            }

            private void readAndCheck(MRPhoto target) {
                MRPhotoId targetId = target.getId();
                Query query = queries.byIds(MRPhoto.class, ImmutableSet.of(targetId));
                checkRead(query, target);
            }

            private void checkRead(Query query, MRPhoto... expected) {
                List<? extends EntityState> readMessages = execute(query);
                assertThat(readMessages, containsInAnyOrder(expected));
            }

            private MRPhoto onePhoto() {
                MRPhoto target = givenPhotos.values()
                                          .stream()
                                          .findFirst()
                                          .orElseGet(() -> fail("No test data."));
                return target;
            }

            private MRPhoto anotherPhoto() {
                MRPhoto target = givenPhotos.values()
                                          .stream()
                                          .skip(1)
                                          .findFirst()
                                          .orElseGet(() -> fail(
                                                  "Too few test data items: " + givenPhotos.size())
                                          );
                return target;
            }

            private void archive(MRPhoto photo) {
                dispatchEvent(archived(photo));
            }

            private void delete(MRPhoto photo) {
                dispatchEvent(deleted(photo));
            }
        }

        @Test
        @DisplayName("for an aggregate type which is not present")
        void notPresent() {
            Query query = queries.all(MRVideo.class);

            Collection<?> result = execute(query);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("dispatch received event")
    class DispatchEvents {

        @Test
        @DisplayName("to nowhere if the event is not on an aggregate update")
        void projection() {
            MRVideoId videoId = MRVideoId.generate();
            TypeUrl projectionType = TypeUrl.of(MRLocalizedVideo.class);
            dispatchStateChanged(projectionType, videoId, MRLocalizedVideo.getDefaultInstance());

            checkNotFound(projectionType);
        }

        @Test
        @DisplayName("to nowhere if the target type is not marked as an `(entity)`")
        @MuteLogging
        void incompleteAggregate() {
            String id = newUuid();
            TypeUrl type = TypeUrl.of(MRIncompleteAudio.class);
            dispatchStateChanged(type, Identifier.pack(id), MRIncompleteAudio.getDefaultInstance());

            checkNotFound(type);
        }

        private void dispatchStateChanged(TypeUrl type, Message id, EntityState state) {
            MessageId historyId = MessageId
                    .newBuilder()
                    .setId(pack(id))
                    .setTypeUrl(type.value())
                    .build();
            EntityStateChanged event = EntityStateChanged
                    .newBuilder()
                    .setEntity(historyId)
                    .setWhen(currentTime())
                    .setOldState(pack(state.getDefaultInstanceForType()))
                    .setNewState(pack(state))
                    .addSignalId(cause())
                    .build();
            dispatchEvent(event(event));
        }

        private void checkNotFound(TypeUrl type) {
            Query query = queries.all(type.getMessageClass());
            List<?> result = execute(query);
            assertTrue(result.isEmpty());
        }
    }

    private void prepareAggregates(Map<MessageId, ? extends EntityState> aggregateStates) {
        aggregateStates.entrySet()
                       .stream()
                       .map(entry -> entityStateChanged(entry.getKey(), entry.getValue()))
                       .forEach(this::dispatchEvent);
    }

    @SuppressWarnings("CheckReturnValue")
    private void dispatchEvent(Event event) {
        EventEnvelope envelope = EventEnvelope.of(event);
        repository.dispatch(envelope);
    }

    private List<? extends EntityState> execute(Query query) {
        Iterator<EntityStateWithVersion> result = repository.execute(query);
        List<? extends EntityState> readMessages =
                Streams.stream(result)
                       .map(EntityStateWithVersion::getState)
                       .map(unpackFunc())
                       .map(EntityState.class::cast)
                       .collect(toList());
        return readMessages;
    }
}
