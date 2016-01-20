/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;

import javax.annotation.Nullable;

import static org.junit.Assert.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class RepositoryShould {

    private BoundedContext boundedContext;
    private Repository<ProjectId, ProjectEntity> repository;
    private EntityStorage<ProjectId> storage;

    @Before
    public void setUp() {
        boundedContext = BoundedContextTestStubs.create();
        repository = new TestRepo(boundedContext);
        storage = InMemoryStorageFactory.getInstance().createEntityStorage(ProjectEntity.class);
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

    public static class EntityWithPrivateConstructor extends Entity<ProjectId, Project> {
        private EntityWithPrivateConstructor(ProjectId id) {
            super(id);
        }
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class RepositoryForEntitiesWithPrivateConstructor extends Repository<ProjectId, EntityWithPrivateConstructor> {
        public RepositoryForEntitiesWithPrivateConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void checkStorageClass(Object storage) {
        }
        @Override
        public void store(EntityWithPrivateConstructor obj) {
        }
        @Nullable
        @Override
        public EntityWithPrivateConstructor load(ProjectId id) {
            return null;
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

    public static class EntityWithProtectedConstructor extends Entity<ProjectId, Project> {

        protected EntityWithProtectedConstructor(ProjectId id) {
            super(id);
        }
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class RepositoryForEntitiesWithProtectedConstructor extends Repository<ProjectId, EntityWithProtectedConstructor> {
        public RepositoryForEntitiesWithProtectedConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void checkStorageClass(Object storage) {
        }
        @Override
        public void store(EntityWithProtectedConstructor obj) {
        }
        @Nullable
        @Override
        public EntityWithProtectedConstructor load(ProjectId id) {
            return null;
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

    public static class EntityWithoutRequiredConstructor extends Entity<ProjectId, Project> {
        private EntityWithoutRequiredConstructor() {
            super(ProjectId.getDefaultInstance());
        }
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class RepositoryForEntitiesWithoutRequiredConstructor extends Repository<ProjectId, EntityWithoutRequiredConstructor> {
        public RepositoryForEntitiesWithoutRequiredConstructor(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void checkStorageClass(Object storage) {
        }
        @Override
        public void store(EntityWithoutRequiredConstructor obj) {
        }
        @Nullable
        @Override
        public EntityWithoutRequiredConstructor load(ProjectId id) {
            return null;
        }
    }

    //
    // Tests of regular work
    //-----------------------

    public static class ProjectEntity extends Entity<ProjectId, Project> {
        public ProjectEntity(ProjectId id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestRepo extends Repository<ProjectId, ProjectEntity> {

        protected TestRepo(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void store(ProjectEntity obj) {}

        @Nullable
        @Override
        protected ProjectEntity load(ProjectId id) {
            return null;
        }

        @Override
        protected void checkStorageClass(Object storage) {}
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
    public void accept_storage() {
        repository.assignStorage(storage);

        assertEquals(storage, repository.getStorage());
        assertTrue(repository.storageAssigned());
    }

    @Test
    public void close_storage_on_close() throws Exception {
        repository.assignStorage(storage);

        repository.close();
        assertTrue(storage.isClosed());
    }

    @Test
    public void disconnect_from_storage_on_close() throws Exception {
        repository.assignStorage(storage);

        repository.close();
        assertFalse(repository.storageAssigned());
        assertNull(repository.getStorage());
    }

    @Test
    public void ignore_if_the_same_storage_is_passed_twice_or_more() {
        repository.assignStorage(storage);

        repository.assignStorage(storage);
        repository.assignStorage(storage);
        assertEquals(storage, repository.getStorage());
    }

    @Test(expected = IllegalStateException.class)
    public void reject_another_passed_storage() {
        repository.assignStorage(storage);

        repository.assignStorage(InMemoryStorageFactory.getInstance().createEntityStorage(ProjectEntity.class));
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

    public static class FailingEntity extends Entity<ProjectId, Project> {
        public FailingEntity(ProjectId id) {
            super(id);
            throw new UnsupportedOperationException("This constructor does not finish by design of this test.");
        }

        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class RepoForFailingEntities extends Repository<ProjectId, FailingEntity> {

        protected RepoForFailingEntities(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected void store(FailingEntity obj) {
        }

        @Nullable
        @Override
        protected FailingEntity load(ProjectId id) {
            return null;
        }

        @Override
        protected void checkStorageClass(Object storage) {
        }
    }
}
