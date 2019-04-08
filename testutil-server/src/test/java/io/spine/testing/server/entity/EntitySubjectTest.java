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

package io.spine.testing.server.entity;

import com.google.common.truth.Subject;
import io.spine.server.entity.Entity;
import io.spine.testing.server.SubjectTest;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BbProjectView;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.entity.testenv.esubject.ProjectView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.ExpectFailure.assertThat;
import static io.spine.testing.server.entity.EntitySubject.ENTITY_SHOULD_EXIST;
import static io.spine.testing.server.entity.EntitySubject.assertEntity;
import static io.spine.testing.server.entity.EntitySubject.entities;

@DisplayName("EntitySubject should")
class EntitySubjectTest extends SubjectTest<EntitySubject, Entity<?, ?>> {

    private Entity<?, ?> entity;

    @Override
    protected Subject.Factory<EntitySubject, Entity<?, ?>> subjectFactory() {
        return entities();
    }

    @BeforeEach
    void setUp() {
        BbProjectId id = BbProjectId.generate();
        BbProjectView state = BbProjectView
                .vBuilder()
                .setId(id)
                .build();
        entity = Given.projectionOfClass(ProjectView.class)
                      .withId(id)
                      .withState(state)
                      .build();
    }

    @Test
    @DisplayName("check if the entity is archived")
    void checkArchived() {
        EntitySubject assertEntity = assertEntity(entity);
        assertEntity.archivedFlag()
                    .isFalse();
        expectSomeFailure(whenTesting -> whenTesting.that(entity)
                                                    .archivedFlag()
                                                    .isTrue());
    }

    @Test
    @DisplayName("check if the entity is deleted")
    void checkDeleted() {
        EntitySubject assertEntity = assertEntity(entity);
        assertEntity.deletedFlag()
                    .isFalse();
        expectSomeFailure(whenTesting -> whenTesting.that(entity)
                                                    .deletedFlag()
                                                    .isTrue());
    }

    @Test
    @DisplayName("produce a subject for the entity state")
    void verifyState() {
        assertEntity(entity).hasStateThat()
                            .isEqualTo(entity.state());
    }

    @Test
    @DisplayName("check that the entity exists")
    void checkExists() {
        assertEntity(entity).exists();
        AssertionError failure = expectFailure(whenTesting -> whenTesting.that(entity)
                                                                         .doesNotExist());
        assertThat(failure).factValue(EXPECTED).isEqualTo(NULL);
    }

    @Test
    @DisplayName("check that the entity does not exist")
    void checkDoesNotExist() {
        assertEntity(null).doesNotExist();
        AssertionError failure = expectFailure(whenTesting -> whenTesting.that(null)
                                                                         .exists());
        assertThat(failure).factValue(EXPECTED_NOT_TO_BE).isEqualTo(NULL);
    }

    @Nested
    @DisplayName("if entity does not exist, fail to check")
    class OnNonExisting {

        @Test
        @DisplayName("if archived")
        void archived() {
            @SuppressWarnings("CheckReturnValue")
            AssertionError error = expectFailure(whenTesting -> whenTesting.that(null)
                                                                           .archivedFlag());
            assertThat(error).factKeys().contains(ENTITY_SHOULD_EXIST);
        }

        @Test
        @DisplayName("if deleted")
        void deleted() {
            @SuppressWarnings("CheckReturnValue")
            AssertionError error = expectFailure(whenTesting -> whenTesting.that(null)
                                                                           .deletedFlag());
            assertThat(error).factKeys().contains(ENTITY_SHOULD_EXIST);
        }

        @Test
        @DisplayName("entity state")
        void state() {
            @SuppressWarnings("CheckReturnValue")
            AssertionError error = expectFailure(whenTesting -> whenTesting.that(null)
                                                                           .hasStateThat());
            assertThat(error).factKeys().contains(ENTITY_SHOULD_EXIST);
        }
    }
}
