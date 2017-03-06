/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.entity.Project;
import org.spine3.test.entity.ProjectId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

public class RepositoryShould {

    private BoundedContext boundedContext;
    private Repository<ProjectId, ProjectEntity> repository;
    private StorageFactory storageFactory;

    @Before
    public void setUp() {
        boundedContext = newBoundedContext();
        repository = new TestRepo(boundedContext);
        storageFactory = InMemoryStorageFactory.getInstance();
    }

    //
    // Tests of initialization checks
    //-------------------------

    @Test(expected = IllegalStateException.class)
    public void check_for_entity_id_class() {
        new RepoForEntityWithUnsupportedId(boundedContext).getIdClass();
    }

    private static class EntityWithUnsupportedId
            extends AbstractVersionableEntity<Exception, Project> {
        protected EntityWithUnsupportedId(Exception id) {
            super(id);
        }
    }

    @SuppressWarnings("ReturnOfNull")
    private static class RepoForEntityWithUnsupportedId
            extends Repository<Exception, EntityWithUnsupportedId> {

        /**
         * Creates the repository in the passed {@link BoundedContext}.
         *
         * @param boundedContext the {@link BoundedContext} in which this repository works
         */
        protected RepoForEntityWithUnsupportedId(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        public Optional<EntityWithUnsupportedId> load(Exception id) {
            return null;
        }

        @Override
        public EntityWithUnsupportedId create(Exception id) {
            return null;
        }

        @Override
        protected void store(EntityWithUnsupportedId obj) {

        }

        @Override
        protected void markArchived(Exception id) {
        }

        @Override
        protected void markDeleted(Exception id) {
        }

        @Override
        protected Storage<Exception, ?> createStorage(StorageFactory factory) {
            return null;
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_constructor_is_private() {
        try {
            new RepositoryForEntitiesWithPrivateConstructor(boundedContext);
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), IllegalAccessException.class);
        }
    }

    private static class EntityWithPrivateConstructor
            extends AbstractVersionableEntity<ProjectId, Project> {
        private EntityWithPrivateConstructor(ProjectId id) {
            super(id);
        }
    }

    @SuppressWarnings("ReturnOfNull")
    private static class RepositoryForEntitiesWithPrivateConstructor
            extends Repository<ProjectId, EntityWithPrivateConstructor> {
        private RepositoryForEntitiesWithPrivateConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        public EntityWithPrivateConstructor create(ProjectId id) {
            return null;
        }

        @Override
        protected Storage<ProjectId, ?> createStorage(StorageFactory factory) {
            return null;
        }

        @Override
        public void store(EntityWithPrivateConstructor obj) {
        }

        @Override
        public Optional<EntityWithPrivateConstructor> load(ProjectId id) {
            return Optional.absent();
        }

        @Override
        protected void markArchived(ProjectId id) {
        }

        @Override
        protected void markDeleted(ProjectId id) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_constructor_is_protected() {
        try {
            new RepositoryForEntitiesWithProtectedConstructor(boundedContext);
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), IllegalAccessException.class);
        }
    }

    private static class EntityWithProtectedConstructor
            extends AbstractVersionableEntity<ProjectId, Project> {
        protected EntityWithProtectedConstructor(ProjectId id) {
            super(id);
        }
    }

    @SuppressWarnings("ReturnOfNull")
    private static class RepositoryForEntitiesWithProtectedConstructor
            extends Repository<ProjectId, EntityWithProtectedConstructor> {
        public RepositoryForEntitiesWithProtectedConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        public EntityWithProtectedConstructor create(ProjectId id) {
            return null;
        }

        @Override
        protected Storage<ProjectId, ?> createStorage(StorageFactory factory) {
            return null;
        }

        @Override
        public void store(EntityWithProtectedConstructor obj) {
        }

        @Override
        public Optional<EntityWithProtectedConstructor> load(ProjectId id) {
            return Optional.absent();
        }

        @Override
        protected void markArchived(ProjectId id) {
        }

        @Override
        protected void markDeleted(ProjectId id) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_has_no_required_constructor() {
        try {
            new RepositoryForEntitiesWithoutRequiredConstructor(boundedContext);
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), NoSuchMethodException.class);
        }
    }

    private static class EntityWithoutRequiredConstructor
            extends AbstractVersionableEntity<ProjectId, Project> {
        private EntityWithoutRequiredConstructor() {
            super(ProjectId.getDefaultInstance());
        }
    }

    @SuppressWarnings("ReturnOfNull")
    private static class RepositoryForEntitiesWithoutRequiredConstructor
            extends Repository<ProjectId, EntityWithoutRequiredConstructor> {
        private RepositoryForEntitiesWithoutRequiredConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        public EntityWithoutRequiredConstructor create(ProjectId id) {
            return null;
        }

        @Override
        protected Storage<ProjectId, ?> createStorage(StorageFactory factory) {
            return null;
        }

        @Override
        public void store(EntityWithoutRequiredConstructor obj) {
        }

        @Override
        public Optional<EntityWithoutRequiredConstructor> load(ProjectId id) {
            return Optional.absent();
        }

        @Override
        protected void markArchived(ProjectId id) {
        }

        @Override
        protected void markDeleted(ProjectId id) {
        }
    }

    //
    // Tests of regular work
    //-----------------------

    private static class ProjectEntity extends AbstractVersionableEntity<ProjectId, Project> {
        private ProjectEntity(ProjectId id) {
            super(id);
        }
    }

    private static class TestRepo extends Repository<ProjectId, ProjectEntity> {

        private TestRepo(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @SuppressWarnings("ReturnOfNull")
        @Override
        public ProjectEntity create(ProjectId id) {
            return null;
        }

        @Override
        protected void store(ProjectEntity obj) {}

        @Override
        public Optional<ProjectEntity> load(ProjectId id) {
            return Optional.absent();
        }

        @Override
        protected void markArchived(ProjectId id) {
        }

        @Override
        protected void markDeleted(ProjectId id) {
        }

        @Override
        protected Storage<ProjectId, ?> createStorage(StorageFactory factory) {
            return factory.createRecordStorage(getEntityClass());
        }
    }

    @Test
    public void return_its_BoundedContext() {
        assertEquals(boundedContext, repository.getBoundedContext());
    }

    @Test
    public void have_no_storage_upon_creation() {
        assertFalse(repository.storageAssigned());
        assertNull(repository.getStorage());
    }

    @Test
    public void init_storage_with_factory() {
        repository.initStorage(storageFactory);
        assertTrue(repository.storageAssigned());
        assertNotNull(repository.getStorage());
    }

    @Test(expected = IllegalStateException.class)
    public void allow_initializing_storage_only_once() {
        repository.initStorage(storageFactory);
        repository.initStorage(storageFactory);
    }

    @Test
    public void close_storage_on_close() throws Exception {
        repository.initStorage(storageFactory);

        final RecordStorage<?> storage = (RecordStorage<?>) repository.getStorage();
        repository.close();

        //noinspection ConstantConditions
        assertTrue(storage.isClosed());
        assertNull(repository.getStorage());
    }

    @Test
    public void disconnect_from_storage_on_close() throws Exception {
        repository.initStorage(storageFactory);

        repository.close();
        assertFalse(repository.storageAssigned());
        assertNull(repository.getStorage());
    }
}
