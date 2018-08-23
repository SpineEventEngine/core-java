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

package io.spine.server.entity.given;

import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.DefaultRecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;

import java.util.Optional;

/**
 * @author Alexander Yevsyukov
 */
public class RepositoryTestEnv {

    private RepositoryTestEnv() {
        // Prevent instantiation of this utility class.
    }

    private static class EntityWithUnsupportedId
            extends AbstractVersionableEntity<Exception, Project> {
        protected EntityWithUnsupportedId(Exception id) {
            super(id);
        }
    }

    @SuppressWarnings("ReturnOfNull")
    public static class RepoForEntityWithUnsupportedId
            extends Repository<Exception, EntityWithUnsupportedId> {

        public RepoForEntityWithUnsupportedId() {
            super();
        }

        @Override
        public Optional<EntityWithUnsupportedId> find(Exception id) {
            return null;
        }

        @Override
        protected EntityWithUnsupportedId instantiate(Exception id) {
            return null;
        }

        @Override
        protected void store(EntityWithUnsupportedId obj) {
        }

        @Override
        protected Storage<Exception, ?, ?> createStorage(StorageFactory factory) {
            return null;
        }
    }

    public static class ProjectEntity extends AbstractVersionableEntity<ProjectId, Project> {
        private ProjectEntity(ProjectId id) {
            super(id);
        }
    }

    public static class TestRepo
            extends DefaultRecordBasedRepository<ProjectId, ProjectEntity, Project> {

        /**
         * The ID value to simulate failure to load an entity.
         */
        private static final ProjectId troublesome = ProjectId.newBuilder()
                                                              .setId("CANNOT_BE_LOADED")
                                                              .build();

        @Override
        public Optional<ProjectEntity> find(ProjectId id) {
            if (id.equals(troublesome)) {
                return Optional.empty();
            }
            return super.find(id);
        }
    }
}
