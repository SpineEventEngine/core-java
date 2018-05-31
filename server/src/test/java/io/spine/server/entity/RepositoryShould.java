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

package io.spine.server.entity;

import com.google.common.base.Predicates;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.entity.given.RepositoryTestEnv.ProjectEntity;
import io.spine.server.entity.given.RepositoryTestEnv.RepoForEntityWithUnsupportedId;
import io.spine.server.entity.given.RepositoryTestEnv.TestRepo;
import io.spine.server.model.ModelError;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.entity.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.given.GivenTenantId.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryShould {

    private BoundedContext boundedContext;
    private Repository<ProjectId, ProjectEntity> repository;
    private StorageFactory storageFactory;
    private TenantId tenantId;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static ProjectId createId(String value) {
        return ProjectId.newBuilder()
                        .setId(value)
                        .build();
    }

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        repository = new TestRepo();
        storageFactory = boundedContext.getStorageFactory();
        tenantId = newUuid();
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
    }

    /*
     * Tests of initialization
     **************************/

    @Test
    public void check_for_entity_id_class() {
        thrown.expect(ModelError.class);
        new RepoForEntityWithUnsupportedId().getIdClass();
    }

    @Test
    public void report_unregistered_on_init() {
        assertFalse(new TestRepo().isRegistered());
    }

    @Test
    public void not_allow_getting_BoundedContext_before_registration() {
        thrown.expect(IllegalStateException.class);
        new TestRepo().getBoundedContext();
    }

    /*
     * Tests of regular work
     **************************/

    @Test
    public void reject_repeated_storage_initialization() {
        repository.initStorage(storageFactory);

        thrown.expect(IllegalStateException.class);
        repository.initStorage(storageFactory);
    }

    @Test
    public void have_no_storage_upon_creation() {
        assertFalse(repository.isStorageAssigned());
    }

    @Test
    public void prohibit_obtaining_unassigned_storage() {
        thrown.expect(IllegalStateException.class);
        repository.getStorage();
    }

    @Test
    public void init_storage_with_factory() {
        repository.initStorage(storageFactory);
        assertTrue(repository.isStorageAssigned());
        assertNotNull(repository.getStorage());
    }

    @Test
    public void close_storage_on_close() {
        repository.initStorage(storageFactory);

        final RecordStorage<?> storage = (RecordStorage<?>) repository.getStorage();
        repository.close();

        assertTrue(storage.isClosed());
        assertFalse(repository.isStorageAssigned());
    }

    @Test
    public void disconnect_from_storage_on_close() {
        repository.initStorage(storageFactory);

        repository.close();
        assertFalse(repository.isStorageAssigned());
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
                final List<ProjectEntity> entities = newArrayList(getIterator(tenantId));
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
                        return repository.iterator(Predicates.alwaysTrue());
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

    private void createAndStore(String entityId) {
        ProjectEntity entity = repository.create(createId(entityId));
        repository.store(entity);
    }
}
