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

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.client.CommandFactory;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv;
import io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.InvisibleSound;
import io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.PhotoAggregate;
import io.spine.server.entity.EntityRecord;
import io.spine.system.server.MRUploadPhoto;
import io.spine.test.system.server.MRPhoto;
import io.spine.test.system.server.MRPhotoId;
import io.spine.test.system.server.MRSoundRecord;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.EntityQueryToProto.transformWith;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpackFunc;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.archive;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.delete;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.givenPhotos;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.jxBrowserLogo7K;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.newPhotosRepository;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.projectLogo1000by800;
import static io.spine.server.aggregate.given.mirror.AggregateMirroringTestEnv.spineLogo200by200;
import static io.spine.server.entity.storage.EntityRecordColumn.archived;
import static io.spine.server.entity.storage.EntityRecordColumn.deleted;
import static io.spine.test.system.server.MRPhotoType.CROP_FRAME;
import static io.spine.test.system.server.MRPhotoType.FULL_FRAME;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`AggregateRepository` should mirror aggregate states")
class AggregateMirroringTest {

    private QueryFactory queries;
    private Collection<MRPhoto> givenPhotos;
    private BoundedContext context;
    private CommandFactory commands;
    private AggregateRepository<MRPhotoId, PhotoAggregate, MRPhoto> repository;

    @BeforeEach
    void setUp() {
        repository = newPhotosRepository();
        context = BoundedContextBuilder.assumingTests(false)
                                       .add(repository)
                                       .build();
        TestActorRequestFactory requestFactory =
                new TestActorRequestFactory(AggregateMirroringTest.class);
        queries = requestFactory.query();
        commands = requestFactory.command();
        givenPhotos = givenPhotos();
        prepareAggregates(givenPhotos);
    }

    @Nested
    @DisplayName("returning them when querying")
    class ExecuteQueries {

        @Test
        @DisplayName("all instances")
        void includeAll() {
            Query query = MRPhoto.query()
                                 .build(transformWith(queries));
            List<? extends EntityState<?>> readMessages = execute(query);
            assertThat(readMessages).containsExactlyElementsIn(givenPhotos);
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
            Query query =
                    MRPhoto.query()
                           .id().is(targetId)
                           .where(archived.lifecycle(), true)
                           .build(transformWith(queries));
            checkRead(query, target);
        }

        @Test
        @DisplayName("by instance ID if the aggregate is deleted")
        void deletedInstance() {
            MRPhoto target = onePhoto();
            deleteItem(target);
            MRPhotoId targetId = target.getId();
            Query query =
                    MRPhoto.query()
                           .id().is(targetId)
                           .where(deleted.lifecycle(), true)
                           .build(transformWith(queries));
            checkRead(query, target);
        }

        @Test
        @DisplayName("all archived instances")
        void allArchived() {
            MRPhoto firstPhoto = onePhoto();
            MRPhoto secondPhoto = anotherPhoto();
            archiveItem(firstPhoto);
            archiveItem(secondPhoto);
            Query query =
                    MRPhoto.query()
                           .where(archived.lifecycle(), true)
                           .where(deleted.lifecycle(), false)
                           .build(transformWith(queries));
            checkRead(query, firstPhoto, secondPhoto);
        }

        @Test
        @DisplayName("all deleted instances")
        void allDeleted() {
            MRPhoto firstPhoto = onePhoto();
            MRPhoto secondPhoto = anotherPhoto();
            deleteItem(firstPhoto);
            deleteItem(secondPhoto);
            Query query = MRPhoto.query()
                                 .where(archived.lifecycle(), false)
                                 .where(deleted.lifecycle(), true)
                                 .build(transformWith(queries));
            checkRead(query, firstPhoto, secondPhoto);
        }

        @Test
        @DisplayName("by the a single entity column")
        void bySingleEntityColumn() {
            Query queryForNothing =
                    MRPhoto.query()
                           .height().isLessThan(20)
                           .build(transformWith(queries));
            checkReadsNothing(queryForNothing);

            Query queryForOneElement =
                    MRPhoto.query()
                           .height().isLessThan(201)
                           .build(transformWith(queries));
            checkRead(queryForOneElement, spineLogo200by200());
        }

        @Test
        @DisplayName("by the two entity columns joined with `AND`")
        void byTwoEntityColumnsWithAndOperator() {
            Query query =
                    MRPhoto.query()
                           .height().isGreaterOrEqualTo(7000)
                           .width().isGreaterThan(6999)
                           .build(transformWith(queries));
            checkRead(query, jxBrowserLogo7K());
        }

        @Test
        @DisplayName("by the two entity columns joined with `OR`")
        void byTwoEntityColumnsWithOrOperator() {
            Query query =
                    MRPhoto.query()
                           .either(q -> q.width().isLessOrEqualTo(200),
                                   q -> q.height().isGreaterThan(6999))
                           .build(transformWith(queries));
            checkRead(query, spineLogo200by200(), jxBrowserLogo7K());
        }

        @Test
        @DisplayName("by the two parameters for the same columns with `OR` " +
                "and by one more column joined with `AND`")
        void byCombinationAndOr() {
            Query query =
                    MRPhoto.query()
                           .either(q -> q.sourceType().is(CROP_FRAME),
                                   q -> q.sourceType().is(FULL_FRAME))
                           .height().isLessThan(7000)
                           .build(transformWith(queries));
            checkRead(query, projectLogo1000by800());
        }

        @Test
        @DisplayName("by both entity and lifecycle columns")
        void byEntityAndLifecycleCols() {
            archiveItem(projectLogo1000by800());
            Query query =
                    MRPhoto.query()
                           .either(q -> q.where(archived.lifecycle(), true),
                                   q -> q.height().isGreaterThan(1000))
                           .width().isLessThan(7000)
                           .build(transformWith(queries));
            checkRead(query, projectLogo1000by800());
        }

        private void readAndCheck(MRPhoto target) {
            MRPhotoId targetId = target.getId();
            Query query = MRPhoto.query()
                                 .id().is(targetId)
                                 .build(transformWith(queries));
            checkRead(query, target);
        }

        private void checkRead(Query query, MRPhoto... expected) {
            List<? extends EntityState<?>> readMessages = execute(query);
            assertThat(readMessages).containsExactlyElementsIn(expected);
        }

        private void checkReadsNothing(Query query) {
            List<? extends EntityState<?>> readMessages = execute(query);
            assertThat(readMessages).isEmpty();
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
                                                "Too few test data items: " + givenPhotos.size())
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
            Iterator<EntityRecord> records = repository.findRecords(query.filters(),
                                                                    query.responseFormat());
            ImmutableList<EntityRecord> asList = ImmutableList.copyOf(records);
            List<? extends EntityState<?>> result =
                    asList.stream()
                          .map(EntityRecord::getState)
                          .map(unpackFunc())
                          .map((s) -> (EntityState<?>) s)
                          .collect(toList());
            return result;
        }
    }

    @Test
    @DisplayName("only for those Aggregate types which are not invisible")
    void onlyForVisible() {
        Query query = MRSoundRecord.query().build(transformWith(queries));
        assertThrows(IllegalStateException.class,
                     () -> InvisibleSound.repository()
                                         .findRecords(query.filters(), query.responseFormat()));
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
