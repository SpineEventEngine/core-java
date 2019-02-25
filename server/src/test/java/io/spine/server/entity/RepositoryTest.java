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

package io.spine.server.entity;

import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.entity.given.repository.ProjectEntity;
import io.spine.server.entity.given.repository.RepoForEntityWithUnsupportedId;
import io.spine.server.entity.given.repository.TestRepo;
import io.spine.server.model.ModelError;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.test.entity.ProjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static com.google.common.collect.Iterators.size;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Repository should")
class RepositoryTest {

    private BoundedContext boundedContext;
    private Repository<ProjectId, ProjectEntity> repository;
    private StorageFactory storageFactory;
    private TenantId tenantId;

    private static ProjectId createId(String value) {
        return ProjectId.newBuilder()
                        .setId(value)
                        .build();
    }

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        repository = new TestRepo();
        storageFactory = boundedContext.storageFactory();
        tenantId = newUuid();
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    @DisplayName("check for entity ID class")
    void checkEntityIdType() {
        assertThrows(ModelError.class, () -> new RepoForEntityWithUnsupportedId().idClass());
    }

    @Test
    @DisplayName("report unregistered on init")
    void beUnregisteredOnInit() {
        assertFalse(new TestRepo().isRegistered());
    }

    @Test
    @DisplayName("not allow getting BoundedContext before registration")
    void notGetBcIfUnregistered() {
        assertThrows(IllegalStateException.class, () -> new TestRepo().boundedContext());
    }

    @Test
    @DisplayName("reject repeated storage initialization")
    void notInitStorageRepeatedly() {
        repository.initStorage(storageFactory);

        assertThrows(IllegalStateException.class, () -> repository.initStorage(storageFactory));
    }

    @Test
    @DisplayName("have no storage upon creation")
    void haveNoStorageOnCreation() {
        assertFalse(repository.isStorageAssigned());
    }

    @Test
    @DisplayName("prohibit obtaining unassigned storage")
    void notGetUnassignedStorage() {
        assertThrows(IllegalStateException.class, () -> repository.storage());
    }

    @Test
    @DisplayName("init storage with factory")
    void initStorageWithFactory() {
        repository.initStorage(storageFactory);
        assertTrue(repository.isStorageAssigned());
        assertNotNull(repository.storage());
    }

    @Test
    @DisplayName("provide default event filter")
    void provideDefaultEventFilter() {
        EventFilter filter = repository.eventFilter();
        assertSame(EventFilter.allowAll(), filter);
    }

    @Test
    @DisplayName("close storage on close")
    void closeStorageOnClose() {
        repository.initStorage(storageFactory);

        RecordStorage<?> storage = (RecordStorage<?>) repository.storage();
        repository.close();

        assertTrue(storage.isClosed());
        assertFalse(repository.isStorageAssigned());
    }

    @Test
    @DisplayName("disconnect from storage on close")
    void disconnectStorageOnClose() {
        repository.initStorage(storageFactory);

        repository.close();
        assertFalse(repository.isStorageAssigned());
    }

    /**
     * Creates three entities in the repository.
     */
    private void createAndStoreEntities() {
        TenantAwareOperation op = new TenantAwareOperation(tenantId) {
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
    @DisplayName("iterate over entities")
    void iterateOverEntities() {
        createAndStoreEntities();
        int numEntities = size(getIterator(tenantId));
        assertEquals(3, numEntities);
    }

    @Test
    @DisplayName("not allow removal in entities iterator")
    void notAllowRemovalInIterator() {
        createAndStoreEntities();
        Iterator<ProjectEntity> iterator = getIterator(tenantId);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    private Iterator<ProjectEntity> getIterator(TenantId tenantId) {
        Iterator<ProjectEntity> result =
                TenantAwareRunner.with(tenantId)
                                 .evaluate(() -> repository.iterator(entity -> true));
        return result;
    }

    private void createAndStore(String entityId) {
        ProjectEntity entity = repository.create(createId(entityId));
        repository.store(entity);
    }
}
