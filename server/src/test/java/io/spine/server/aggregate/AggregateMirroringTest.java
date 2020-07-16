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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableSet;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.client.CommandFactory;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.core.Command;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv;
import io.spine.system.server.MRUploadPhoto;
import io.spine.test.system.server.MRPhoto;
import io.spine.test.system.server.MRPhotoId;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static io.spine.client.Filters.eq;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpackFunc;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.archive;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.delete;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.givenPhotos;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.newPhotosRepository;
import static io.spine.server.entity.storage.EntityRecordColumn.archived;
import static io.spine.server.entity.storage.EntityRecordColumn.deleted;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.fail;

//TODO:2020-04-08:alex.tymchenko: test the disabled mirroring.
@DisplayName("`AggregateRepository` should mirror aggregate states")
class AggregateMirroringTest {

    private QueryFactory queries;
    private Collection<MRPhoto> givenPhotos;
    private BoundedContext context;
    private CommandFactory commands;

    @BeforeEach
    void setUp() {
        context = BoundedContextBuilder.assumingTests(false)
                                       .add(newPhotosRepository())
                                       .build();
        TestActorRequestFactory requestFactory =
                new TestActorRequestFactory(AggregateMirroringTest.class);
        queries = requestFactory.query();
        commands = requestFactory.command();
        givenPhotos = givenPhotos();
        prepareAggregates(givenPhotos);
    }

    @Nested
    @DisplayName("returning them via `Stand` when querying")
    class ExecuteQueries {

        @Test
        @DisplayName("all instances")
        void includeAll() {
            Query query = queries.all(MRPhoto.class);
            List<? extends EntityState<?>> readMessages = execute(query);
            assertThat(readMessages, containsInAnyOrder(givenPhotos.toArray()));
        }

        @Test
        @DisplayName("an instance by ID")
        void byId() {
            MRPhoto target = onePhoto();
            readAndCheck(target);
        }

        @Test
        @DisplayName("by instance ID if the aggregate is archived")
        void archivedInstance() {
            MRPhoto target = onePhoto();
            archiveItem(target);
            MRPhotoId targetId = target.getId();
            Query query = queries.select(MRPhoto.class)
                                 .byId(targetId)
                                 .where(eq(archived.name(), true))
                                 .build();
            checkRead(query, target);
        }

        @Test
        @DisplayName("by instance ID if the aggregate is deleted")
        void deletedInstance() {
            MRPhoto target = onePhoto();
            deleteItem(target);
            MRPhotoId targetId = target.getId();
            Query query = queries.select(MRPhoto.class)
                                 .byId(targetId)
                                 .where(eq(deleted.name(), true))
                                 .build();
            checkRead(query, target);
        }

        @Test
        @DisplayName("all archived instances")
        void allArchived() {
            MRPhoto firstPhoto = onePhoto();
            MRPhoto secondPhoto = anotherPhoto();
            archiveItem(firstPhoto);
            archiveItem(secondPhoto);

            Query query = queries.select(MRPhoto.class)
                                 .where(eq(archived.name(), true),
                                        eq(deleted.name(), false))
                                 .build();
            checkRead(query, firstPhoto, secondPhoto);
        }

        @Test
        @DisplayName("all deleted instances")
        void allDeleted() {
            MRPhoto firstPhoto = onePhoto();
            MRPhoto secondPhoto = anotherPhoto();
            deleteItem(firstPhoto);
            deleteItem(secondPhoto);

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
            List<? extends EntityState<?>> readMessages = execute(query);
            assertThat(readMessages, containsInAnyOrder(expected));
        }

        private MRPhoto onePhoto() {
            MRPhoto target = givenPhotos.stream()
                                        .findFirst()
                                        .orElseGet(() -> fail("No test data."));
            return target;
        }

        private MRPhoto anotherPhoto() {
            MRPhoto target = givenPhotos.stream()
                                        .skip(1)
                                        .findFirst()
                                        .orElseGet(() -> fail(
                                                "Too few test data items: " +
                                                        givenPhotos.size())
                                        );
            return target;
        }

        private void archiveItem(MRPhoto photo) {
            dispatchCommand(archive(photo));
        }

        private void deleteItem(MRPhoto photo) {
            dispatchCommand(delete(photo));
        }

        private List<? extends EntityState<?>> execute(Query query) {
            MemoizingObserver<QueryResponse> observer = new MemoizingObserver<>();
            context.stand()
                   .execute(query, observer);
            QueryResponse response = observer.responses()
                                             .get(0);
            List<EntityStateWithVersion> result = response.getMessageList();
            List<? extends EntityState<?>> readMessages =
                    result.stream()
                          .map(EntityStateWithVersion::getState)
                          .map(unpackFunc())
                          .map((s) -> (EntityState<?>) s)
                          .collect(toList());
            return readMessages;
        }
    }

    private void prepareAggregates(Collection<MRPhoto> aggregateStates) {
        for (MRPhoto state : aggregateStates) {
            MRUploadPhoto upload = AggregateMirroringTestEnv.upload(state);
            dispatchCommand(upload);
        }
    }

    private void dispatchCommand(CommandMessage message) {
        Command command = commands.create(message);
        context.commandBus()
               .post(command, noOpObserver());
    }
}
