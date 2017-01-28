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

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

@SuppressWarnings("InstanceMethodNamingConvention")
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

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_constructor_is_private() {
        try {
            new RepositoryForEntitiesWithPrivateConstructor(boundedContext);
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), IllegalAccessException.class);
        }
    }

    private static class EntityWithPrivateConstructor extends Entity<ProjectId, Project> {
        private EntityWithPrivateConstructor(ProjectId id) {
            super(id);
        }
    }

    private static class RepositoryForEntitiesWithPrivateConstructor
            extends Repository<ProjectId, EntityWithPrivateConstructor> {
        private RepositoryForEntitiesWithPrivateConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @SuppressWarnings("ReturnOfNull")
        @Override
        protected Storage createStorage(StorageFactory factory) {
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
        protected boolean markArchived(ProjectId id) {
            return false;
        }

        @Override
        protected boolean markDeleted(ProjectId id) {
            return false;
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

    private static class EntityWithProtectedConstructor extends Entity<ProjectId, Project> {
        protected EntityWithProtectedConstructor(ProjectId id) {
            super(id);
        }
    }

    private static class RepositoryForEntitiesWithProtectedConstructor
            extends Repository<ProjectId, EntityWithProtectedConstructor> {
        public RepositoryForEntitiesWithProtectedConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @SuppressWarnings("ReturnOfNull")
        @Override
        protected Storage createStorage(StorageFactory factory) {
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
        protected boolean markArchived(ProjectId id) {
            return false;
        }

        @Override
        protected boolean markDeleted(ProjectId id) {
            return false;
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

    private static class EntityWithoutRequiredConstructor extends Entity<ProjectId, Project> {
        private EntityWithoutRequiredConstructor() {
            super(ProjectId.getDefaultInstance());
        }
    }

    private static class RepositoryForEntitiesWithoutRequiredConstructor
            extends Repository<ProjectId, EntityWithoutRequiredConstructor> {
        private RepositoryForEntitiesWithoutRequiredConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @SuppressWarnings("ReturnOfNull")
        @Override
        protected Storage createStorage(StorageFactory factory) {
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
        protected boolean markArchived(ProjectId id) {
            return false;
        }

        @Override
        protected boolean markDeleted(ProjectId id) {
            return false;
        }
    }

    //
    // Tests of regular work
    //-----------------------

    private static class ProjectEntity extends Entity<ProjectId, Project> {
        public ProjectEntity(ProjectId id) {
            super(id);
        }
    }

    private static class TestRepo extends Repository<ProjectId, ProjectEntity> {

        private TestRepo(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void store(ProjectEntity obj) {}

        @Override
        protected Optional<ProjectEntity> load(ProjectId id) {
            return Optional.absent();
        }

        @Override
        protected boolean markArchived(ProjectId id) {
            return false;
        }

        @Override
        protected boolean markDeleted(ProjectId id) {
            return false;
        }

        @Override
        protected Storage createStorage(StorageFactory factory) {
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

    @Test
    public void create_entities() {
        final ProjectId id = ProjectId.newBuilder().setId("create_entities()").build();
        final ProjectEntity projectEntity = repository.create(id);
        assertNotNull(projectEntity);
        assertEquals(id, projectEntity.getId());
    }

    @Test(expected = RuntimeException.class)
    public void propagate_exception_if_entity_construction_fails() {
        final Repository<ProjectId, FailingEntity> repo = new RepoForFailingEntities(boundedContext);
        repo.create(ProjectId.newBuilder().setId("works?").build());
    }

    private static class FailingEntity extends Entity<ProjectId, Project> {
        private FailingEntity(ProjectId id) {
            super(id);
            throw new UnsupportedOperationException("This constructor does not finish by design of this test.");
        }
    }

    private static class RepoForFailingEntities extends Repository<ProjectId, FailingEntity> {

        private RepoForFailingEntities(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void store(FailingEntity obj) {
        }

        @SuppressWarnings("ReturnOfNull") // It's the purpose of the test.
        @Nullable
        @Override
        protected Optional<FailingEntity> load(ProjectId id) {
            return null;
        }

        @Override
        protected boolean markArchived(ProjectId id) {
            return false;
        }

        @Override
        protected boolean markDeleted(ProjectId id) {
            return false;
        }

        @SuppressWarnings("ReturnOfNull")
        @Override
        protected Storage createStorage(StorageFactory factory) {
            return null;
        }
    }
}
