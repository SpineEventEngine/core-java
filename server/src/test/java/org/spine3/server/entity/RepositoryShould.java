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
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.server.tenant.TenantAwareOperation;
import org.spine3.test.entity.Project;
import org.spine3.test.entity.ProjectId;
import org.spine3.users.TenantId;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.newTenantUuid;

public class RepositoryShould {

    private BoundedContext boundedContext;
    private Repository<ProjectId, ProjectEntity> repository;
    private StorageFactory storageFactory;
    private TenantId tenantId;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        repository = new TestRepo();
        storageFactory = StorageFactorySwitch.get(boundedContext.isMultitenant());
        tenantId = newTenantUuid();
        //TODO:2017-03-26:alexander.yevsyukov: Have single-tenant version of tests too.
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
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
        private RepoForEntityWithUnsupportedId(BoundedContext boundedContext) {
            super();
        }

        @Override
        public Optional<EntityWithUnsupportedId> find(Exception id) {
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
        protected Storage<Exception, ?> createStorage(StorageFactory factory) {
            return null;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void reject_repeated_storage_initialization() {
        repository.initStorage(storageFactory);
        repository.initStorage(storageFactory);
    }

    //
    // Tests of regular work
    //-----------------------

    private static class ProjectEntity extends AbstractVersionableEntity<ProjectId, Project> {
        private ProjectEntity(ProjectId id) {
            super(id);
        }
    }

    private static class TestRepo
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
                return Optional.absent();
            }
            return super.find(id);
        }
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

    /**
     * Creates three entities in the repository.
     */
    private void createAndStoreEntities() {
        final TenantAwareOperation op = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                repository.initStorage(storageFactory);

                createAndStore("Eins");
                createAndStore("Zwei");
                createAndStore("Drei");
            }
        };
        op.execute();
    }

    @Test
    public void iterate_over_entities() {
        createAndStoreEntities();

        final int numEntities = new TenantAwareFunction0<Integer>(tenantId) {
            @Override
            public Integer apply() {
                final List<ProjectEntity> entities = Lists.newArrayList(getIterator(tenantId));
                return entities.size();
            }
        }.execute();

        assertEquals(3, numEntities);
    }

    private Iterator<ProjectEntity> getIterator(TenantId tenantId) {
        final TenantAwareFunction0<Iterator<ProjectEntity>> op =
                new TenantAwareFunction0<Iterator<ProjectEntity>>(tenantId) {
                    @Override
                    public Iterator<ProjectEntity> apply() {
                        return repository.iterator(Predicates.<ProjectEntity>alwaysTrue());
                    }
                };
        return op.execute();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void do_not_allow_removal_in_iterator() {
        createAndStoreEntities();
        final Iterator<ProjectEntity> iterator = getIterator(tenantId);
        iterator.remove();
    }

    private static ProjectId createId(String value) {
        return ProjectId.newBuilder()
                        .setId(value)
                        .build();
    }

    private void createAndStore(String entityId) {
        ProjectEntity entity = repository.create(createId(entityId));
        repository.store(entity);
    }
}
