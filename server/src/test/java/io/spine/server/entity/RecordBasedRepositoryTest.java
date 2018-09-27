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

import com.google.common.collect.Lists;
import com.google.common.truth.IterableSubject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.storage.RecordStorage;
import io.spine.testing.Tests;
import io.spine.testing.server.model.ModelTests;
import io.spine.testing.server.tenant.TenantAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.TestTransaction.archive;
import static io.spine.server.entity.TestTransaction.delete;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The abstract test for the {@linkplain RecordBasedRepository} derived classes.
 *
 * @param <E> the type of the {@link Entity} of this repository; the type is checked to implement
 *            {@link TestEntityWithStringColumn} at runtime
 * @author Dmytro Dashenkov
 */
public abstract class RecordBasedRepositoryTest<E extends AbstractVersionableEntity<I, S>,
                                                I,
                                                S extends Message>
        extends TenantAwareTest {

    @SuppressWarnings("ProtectedField") // we use the reference in the derived test cases.
    protected RecordBasedRepository<I, E, S> repository;

    private static <E extends AbstractVersionableEntity<?, ?>>
    void assertMatches(E entity, FieldMask fieldMask) {
        Message state = entity.getState();
        Tests.assertMatchesMask(state, fieldMask);
    }

    protected abstract RecordBasedRepository<I, E, S> createRepository();

    protected abstract E createEntity(I id);

    protected abstract List<E> createEntities(int count);

    protected abstract I createId(int value);

    @BeforeEach
    protected void setUp() {
        ModelTests.dropAllModels();
        this.repository = createRepository();
        setCurrentTenant(newUuid());
    }

    @AfterEach
    protected void tearDown() throws Exception {
        clearCurrentTenant();
    }

    /*
     * Store/load functions for working in multi-tenant execution context
     **********************************************************************/

    private void storeEntity(E entity) {
        repository.store(entity);
    }

    @CanIgnoreReturnValue
    private List<E> createAndStoreEntities(RecordBasedRepository<I, E, S> repo, int count) {
        List<E> entities = createEntities(count);
        for (E entity : entities) {
            repo.store(entity);
        }
        return entities;
    }

    private Optional<E> find(I id) {
        return repository.find(id);
    }

    private Iterator<E> loadMany(List<I> ids) {
        return repository.loadAll(ids);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass") // Uses generic param <E> of the top class.
    private Iterator<E> loadAll() {
        return repository.loadAll();
    }

    private E loadOrCreate(I id) {
        return repository.findOrCreate(id);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass") // Uses generic param <E> of the top class.
    private Iterator<E> find(EntityFilters filters, FieldMask firstFieldOnly) {
        return repository.find(filters, firstFieldOnly);
    }

    /*
     * Tests
     ************/

    @Test
    @DisplayName("create entities")
    void createEntities() {
        I id = createId(5);
        E projectEntity = repository.create(id);
        assertNotNull(projectEntity);
        assertEquals(id, projectEntity.getId());
    }

    @Nested
    @DisplayName("find")
    class Find {

        @Test
        @DisplayName("single entity by ID")
        void singleEntityById() {
            E entity = createEntity(createId(985));

            storeEntity(entity);

            Optional<E> optional = find(entity.getId());
            assertTrue(optional.isPresent());

            Entity<?, ?> found = optional.get();
            assertEquals(found, entity);
        }

        @Test
        @DisplayName("multiple entities by IDs")
        void multipleEntitiesByIds() {
            int count = 10;
            List<E> entities = createAndStoreEntities(repository, count);

            List<I> ids = Lists.newLinkedList();

            // Find some of the records (half of them in this case)
            for (int i = 0; i < count / 2; i++) {
                ids.add(entities.get(i)
                                .getId());
            }

            Collection<E> found = newArrayList(loadMany(ids));

            assertThat(found).hasSize(ids.size());
            assertThat(entities).containsAllIn(found);
        }

        @Test
        @DisplayName("entities by query")
        void entitiesByQuery() {
            I id1 = createId(271);
            I id2 = createId(314);
            E entity1 = createEntity(id1);
            E entity2 = createEntity(id2);
            repository.store(entity1);
            repository.store(entity2);

            String fieldPath = "idString";
            StringValue fieldValue = StringValue.newBuilder()
                                                .setValue(id1.toString())
                                                .build();
            ColumnFilter filter = eq(fieldPath, fieldValue);
            CompositeColumnFilter aggregatingFilter = CompositeColumnFilter
                    .newBuilder()
                    .addFilter(filter)
                    .setOperator(ALL)
                    .build();
            EntityFilters filters = EntityFilters
                    .newBuilder()
                    .addFilter(aggregatingFilter)
                    .build();
            Collection<E> found = newArrayList(
                    repository.find(filters, FieldMask.getDefaultInstance())
            );

            IterableSubject assertThatFound = assertThat(found);
            assertThatFound.hasSize(1);
            assertThatFound.contains(entity1);
            assertThatFound.doesNotContain(entity2);
        }

        @Test
        @DisplayName("entities by query and field mask")
        void entitiesByQueryAndFields() {
            int count = 10;
            List<E> entities = createAndStoreEntities(repository, count);

            // Find some of the entities (half of them in this case).
            int idsToObtain = count / 2;
            List<EntityId> ids = obtainSomeNumberOfEntityIds(entities, idsToObtain);

            EntityFilters filters = createEntityIdFilters(ids);
            FieldMask firstFieldOnly = createFirstFieldOnlyMask(entities);
            Iterator<E> readEntities = find(filters, firstFieldOnly);
            Collection<E> foundList = newArrayList(readEntities);

            assertThat(foundList).hasSize(ids.size());
            for (E entity : foundList) {
                assertMatches(entity, firstFieldOnly);
            }
        }

        @Test
        @DisplayName("all entities")
        void allEntities() {
            List<E> entities = createAndStoreEntities(repository, 150);
            Collection<E> found = newArrayList(loadAll());

            IterableSubject assertFoundEntities = assertThat(found);

            assertFoundEntities.hasSize(entities.size());

            assertThat(entities).containsExactlyElementsIn(found);
        }

        @Test
        @DisplayName("no entities if repository is empty")
        void noEntitiesIfEmpty() {
            Collection<E> found = newArrayList(loadAll());
            assertThat(found).isEmpty();
        }

        private List<EntityId> obtainSomeNumberOfEntityIds(List<E> entities, int count) {
            List<EntityId> ids = Lists.newLinkedList();
            for (int i = 0; i < count; i++) {
                Message entityId = (Message) entities.get(i)
                                                     .getId();
                EntityId id = EntityId.newBuilder()
                                      .setId(pack(entityId))
                                      .build();
                ids.add(id);
            }
            return ids;
        }

        private EntityFilters createEntityIdFilters(List<EntityId> ids) {
            EntityIdFilter filter = EntityIdFilter
                    .newBuilder()
                    .addAllIds(ids)
                    .build();
            EntityFilters filters = EntityFilters
                    .newBuilder()
                    .setIdFilter(filter)
                    .build();
            return filters;
        }

        private FieldMask createFirstFieldOnlyMask(List<E> entities) {
            E firstEntity = entities.get(0);
            FieldMask fieldMask = fromFieldNumbers(firstEntity.getDefaultState()
                                                              .getClass(), 1);
            return fieldMask;
        }
    }

    @Test
    @DisplayName("create entity on `loadOrCreate` if not found")
    void loadOrCreateEntity() {
        int count = 3;
        createAndStoreEntities(repository, count);

        I id = createId(count + 1);
        E entity = loadOrCreate(id);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
    }

    @Test
    @DisplayName("handle wrong passed IDs")
    void handleWrongPassedIds() {
        int count = 10;
        List<E> entities = createAndStoreEntities(repository, count);
        List<I> ids = Lists.newLinkedList();
        for (int i = 0; i < count; i++) {
            ids.add(entities.get(i)
                            .getId());
        }
        Entity<I, S> sideEntity = createEntity(createId(375));
        ids.add(sideEntity.getId());

        Collection<E> found = newArrayList(loadMany(ids));
        IterableSubject assertThatFound = assertThat(found);
        assertThatFound.hasSize(ids.size() - 1); // Check we've found all existing items
        assertThatFound.containsExactlyElementsIn(entities);
    }

    @Nested
    @DisplayName("mark records")
    class MarkRecords {

        @Test
        @DisplayName("archived")
        void archived() {
            E entity = createEntity(createId(821));
            I id = entity.getId();

            storeEntity(entity);

            assertTrue(find(id).isPresent());

            entity.setLifecycleFlags(
                    LifecycleFlags.newBuilder()
                                  .setArchived(true)
                                  .build()
            );
            storeEntity(entity);

            assertFalse(find(id).isPresent());
        }

        @Test
        @DisplayName("deleted")
        void deleted() {
            E entity = createEntity(createId(822));
            I id = entity.getId();

            storeEntity(entity);

            assertTrue(find(id).isPresent());

            entity.setLifecycleFlags(
                    LifecycleFlags
                            .newBuilder()
                            .setDeleted(true)
                            .build()
            );
            storeEntity(entity);

            assertFalse(find(id).isPresent());
        }
    }

    @Test
    @DisplayName("exclude non-active records from entity query")
    void excludeNonActiveRecords() {
        I archivedId = createId(42);
        I deletedId = createId(314);
        I activeId = createId(271);

        E activeEntity = repository.create(activeId);
        E archivedEntity = repository.create(archivedId);
        E deletedEntity = repository.create(deletedId);
        delete((TransactionalEntity) deletedEntity);
        archive((TransactionalEntity) archivedEntity);

        // Fill the storage
        repository.store(activeEntity);
        repository.store(archivedEntity);
        repository.store(deletedEntity);

        Iterator<E> found = repository.find(EntityFilters.getDefaultInstance(),
                                            FieldMask.getDefaultInstance());
        List<E> foundList = newArrayList(found);
        // Check results
        assertThat(foundList).hasSize(1);
        E actualEntity = foundList.get(0);
        assertEquals(activeEntity, actualEntity);
    }

    @Test
    @DisplayName("allow any lifecycle if column is involved in query")
    void ignoreLifecycleForColumns() {
        I archivedId = createId(42);
        I deletedId = createId(314);
        I activeId = createId(271);

        E activeEntity = repository.create(activeId);
        E archivedEntity = repository.create(archivedId);
        E deletedEntity = repository.create(deletedId);
        delete((TransactionalEntity) deletedEntity);
        archive((TransactionalEntity) archivedEntity);

        // Fill the storage
        repository.store(activeEntity);
        repository.store(archivedEntity);
        repository.store(deletedEntity);

        CompositeColumnFilter columnFilter = all(eq(archived.name(), false));
        EntityFilters filters = EntityFilters
                .newBuilder()
                .addFilter(columnFilter)
                .build();

        Iterator<E> found = repository.find(filters, FieldMask.getDefaultInstance());
        Collection<E> foundList = newArrayList(found);
        // Check result
        IterableSubject assertFoundList = assertThat(foundList);
        assertFoundList.hasSize(2);
        assertFoundList.containsExactly(activeEntity, deletedEntity);
    }

    @Test
    @DisplayName("cache entity columns on registration")
    void cacheColumnsOnRegister() {
        if (!repository.isRegistered()) {
            repository.onRegistered();
        }

        RecordStorage<I> storage = repository.recordStorage();
        EntityColumnCache entityColumnCache = storage.entityColumnCache();

        // Verify that cache contains searched column
        assertNotNull(entityColumnCache.findColumn("idString"));
    }
}
