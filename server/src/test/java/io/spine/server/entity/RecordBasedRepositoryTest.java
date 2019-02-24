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

import com.google.common.collect.Lists;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.Truth8;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.IdFilter;
import io.spine.client.TargetFilters;
import io.spine.client.TargetFiltersVBuilder;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.storage.RecordStorage;
import io.spine.testing.TestValues;
import io.spine.testing.server.entity.given.GivenLifecycleFlags;
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
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.reverse;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.client.OrderBy.Direction.DESCENDING;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.TestTransaction.archive;
import static io.spine.server.entity.TestTransaction.delete;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.assertMatches;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.emptyFieldMask;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.emptyFilters;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.emptyOrder;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.emptyPagination;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.orderByName;
import static io.spine.server.entity.given.RecordBasedRepositoryTestEnv.pagination;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The abstract test for the {@linkplain RecordBasedRepository} derived classes.
 *
 * @param <E>
 *         the type of the {@link Entity} of this repository; the type is checked to implement
 *         {@link TestEntityWithStringColumn} at runtime
 */
public abstract
class RecordBasedRepositoryTest<E extends AbstractEntity<I, S>, I, S extends Message>
        extends TenantAwareTest {

    private RecordBasedRepository<I, E, S> repository;

    protected abstract RecordBasedRepository<I, E, S> createRepository();

    protected abstract E createEntity(I id);

    protected final E createEntity(int idValue) {
        I id = createId(idValue);
        return createEntity(id);
    }

    protected abstract List<E> createEntities(int count);

    /**
     * Creates the entities using the supplied names for entities
     * {@link io.spine.server.entity.given.RecordBasedRepositoryTestEnv#ENTITY_NAME_COLUMN "name"}
     * state property.
     */
    protected abstract List<E> createNamed(int count, Supplier<String> nameSupplier);

    /**
     * Orders the entities by the
     * {@link io.spine.server.entity.given.RecordBasedRepositoryTestEnv#ENTITY_NAME_COLUMN "name"}
     * state property.
     */
    protected abstract List<E> orderedByName(List<E> entities);

    protected abstract I createId(int value);

    /**
     * Sets the {@code package-local} {@link AbstractEntity#state() state} property of an entity.
     */
    protected void setEntityState(E entity, S state) {
        entity.setState(state);
    }

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
        repository().store(entity);
    }

    @CanIgnoreReturnValue
    private List<E> createAndStoreEntities(RecordBasedRepository<I, E, S> repo, int count) {
        List<E> entities = createEntities(count);
        storeEntities(repo, entities);
        return entities;
    }

    private void storeEntities(RecordBasedRepository<I, E, S> repo, List<E> entities) {
        for (E entity : entities) {
            repo.store(entity);
        }
    }

    private Iterator<E> loadMany(List<I> ids) {
        return repository().loadAll(ids);
    }

    private E loadOrCreate(I id) {
        return repository().findOrCreate(id);
    }

    /*
     * Tests
     ************/

    @Test
    @DisplayName("create entities")
    void createEntities() {
        I id = createId(5);
        E projectEntity = repository().create(id);
        assertNotNull(projectEntity);
        assertEquals(id, projectEntity.id());
    }

    protected RecordBasedRepository<I, E, S> repository() {
        return repository;
    }

    @Nested
    @DisplayName("find one entity")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    class FindOne {

        private E entity;

        @BeforeEach
        void createTheEntity() {
            entity = createEntity(TestValues.random(1000));
        }

        @Test
        @DisplayName("by ID")
        void byId() {
            storeEntity(entity);
            assertFound();
        }

        @Test
        @DisplayName("by ID if archived")
        void byIdArchived() {
            archive((TransactionalEntity) entity);
            storeEntity(entity);
            assertFound();
        }

        @Test
        @DisplayName("by ID if deleted")
        void byIdDeleted() {
            delete((TransactionalEntity) entity);
            storeEntity(entity);
            assertFound();
        }

        private void assertFound() {
            assertResult(find(entity.id()));
        }

        private void assertResult(Optional<E> optional) {
            OptionalSubject assertResult = Truth8.assertThat(optional);
            assertResult.isPresent();
            assertResult.hasValue(entity);
        }

        private Optional<E> find(I id) {
            return repository().find(id);
        }
    }

    @Nested
    @DisplayName("find multiple entities")
    class FindMultiple {

        @Test
        @DisplayName("by IDs")
        void multipleEntitiesByIds() {
            int count = 10;
            List<E> entities = createAndStoreEntities(repository(), count);

            List<I> ids = Lists.newLinkedList();

            // Find some of the records (half of them in this case)
            for (int i = 0; i < count / 2; i++) {
                ids.add(entities.get(i)
                                .id());
            }

            Collection<E> found = newArrayList(loadMany(ids));

            assertThat(found).hasSize(ids.size());
            assertThat(entities).containsAllIn(found);
        }

        @Test
        @DisplayName("by query")
        void entitiesByQuery() {
            I id1 = createId(271);
            I id2 = createId(314);
            E entity1 = createEntity(id1);
            E entity2 = createEntity(id2);
            repository().store(entity1);
            repository().store(entity2);

            String fieldPath = "idString";
            StringValue fieldValue = StringValue.newBuilder()
                                                .setValue(id1.toString())
                                                .build();
            Filter filter = eq(fieldPath, fieldValue);
            CompositeFilter aggregatingFilter = CompositeFilter
                    .newBuilder()
                    .addFilter(filter)
                    .setOperator(ALL)
                    .build();
            TargetFilters filters = TargetFiltersVBuilder
                    .newBuilder()
                    .addFilter(aggregatingFilter)
                    .build();
            Collection<E> found = newArrayList(
                    repository().find(filters, emptyOrder(), emptyPagination(), emptyFieldMask())
            );

            IterableSubject assertThatFound = assertThat(found);
            assertThatFound.hasSize(1);
            assertThatFound.contains(entity1);
            assertThatFound.doesNotContain(entity2);
        }

        @Test
        @DisplayName("by query and field mask")
        void entitiesByQueryAndFields() {
            int count = 10;
            List<E> entities = createAndStoreEntities(repository(), count);

            // Find some of the entities (half of them in this case).
            int idsToObtain = count / 2;
            List<Any> ids = obtainSomeNumberOfEntityIds(entities, idsToObtain);

            TargetFilters filters = createIdFilters(ids);
            FieldMask firstFieldOnly = createFirstFieldOnlyMask(entities);
            Iterator<E> readEntities = find(filters, firstFieldOnly);
            Collection<E> foundList = newArrayList(readEntities);

            assertThat(foundList).hasSize(ids.size());
            for (E entity : foundList) {
                assertMatches(entity, firstFieldOnly);
            }
        }

        @Test
        @DisplayName("in ascending order")
        void entitiesInAscendingOrder() {
            int count = 10;
            // UUIDs are guaranteed to produced a collection with unordered names. 
            List<E> entities = createAndStoreNamed(repository(), count, Identifier::newUuid);

            Iterator<E> readEntities = repository().find(emptyFilters(), orderByName(ASCENDING),
                                                         emptyPagination(), emptyFieldMask());
            Collection<E> foundList = newArrayList(readEntities);

            List<E> expectedList = orderedByName(entities);
            assertThat(foundList).hasSize(count);
            assertEquals(expectedList, foundList);
        }

        @Test
        @DisplayName("in descending order")
        void entitiesInDescendingOrder() {
            int count = 10;
            // UUIDs are guaranteed to produced a collection with unordered names. 
            List<E> entities = createAndStoreNamed(repository(), count, Identifier::newUuid);

            Iterator<E> readEntities = repository().find(emptyFilters(), orderByName(DESCENDING),
                                                         emptyPagination(), emptyFieldMask());
            Collection<E> foundList = newArrayList(readEntities);

            List<E> expectedList = reverse(orderedByName(entities));
            assertThat(foundList).hasSize(count);
            assertEquals(expectedList, foundList);
        }

        @Test
        @DisplayName("limited number of entities")
        void limitedNumberOfEntities() {
            int totalCount = 10;
            int pageSize = 5;
            // UUIDs are guaranteed to produced a collection with unordered names. 
            List<E> entities = createAndStoreNamed(repository(), totalCount, Identifier::newUuid);

            Iterator<E> readEntities = repository().find(emptyFilters(), orderByName(ASCENDING),
                                                         pagination(pageSize), emptyFieldMask());
            Collection<E> foundList = newArrayList(readEntities);

            List<E> expectedList = orderedByName(entities).subList(0, pageSize);
            assertThat(foundList).hasSize(pageSize);
            assertEquals(expectedList, foundList);
        }

        @Test
        @DisplayName("all entities")
        void allEntities() {
            List<E> entities = createAndStoreEntities(repository(), 150);
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

        private Iterator<E> find(TargetFilters filters, FieldMask firstFieldOnly) {
            return repository().find(filters, emptyOrder(), emptyPagination(), firstFieldOnly);
        }

        private List<E> createAndStoreNamed(RecordBasedRepository<I, E, S> repo, int count,
                                            Supplier<String> nameSupplier) {
            List<E> entities = createNamed(count, nameSupplier);
            storeEntities(repo, entities);
            return entities;
        }

        private List<Any> obtainSomeNumberOfEntityIds(List<E> entities, int count) {
            List<Any> ids = Lists.newLinkedList();
            for (int i = 0; i < count; i++) {
                Message entityId = (Message) entities.get(i)
                                                     .id();
                Any id = pack(entityId);
                ids.add(id);
            }
            return ids;
        }

        private TargetFilters createIdFilters(List<Any> ids) {
            IdFilter filter = IdFilter
                    .newBuilder()
                    .addAllIds(ids)
                    .build();
            TargetFilters filters = TargetFiltersVBuilder
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

        private Iterator<E> loadAll() {
            return repository().loadAll();
        }
    }

    @Test
    @DisplayName("create entity on `loadOrCreate` if not found")
    void loadOrCreateEntity() {
        int count = 3;
        createAndStoreEntities(repository(), count);

        I id = createId(count + 1);
        E entity = loadOrCreate(id);

        assertNotNull(entity);
        assertEquals(id, entity.id());
    }

    @Test
    @DisplayName("handle wrong passed IDs")
    void handleWrongPassedIds() {
        int count = 10;
        List<E> entities = createAndStoreEntities(repository(), count);
        List<I> ids = Lists.newLinkedList();
        for (int i = 0; i < count; i++) {
            ids.add(entities.get(i)
                            .id());
        }
        Entity<I, S> sideEntity = createEntity(375);
        ids.add(sideEntity.id());

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
            E entity = createEntity(821);
            I id = entity.id();

            storeEntity(entity);

            assertFound(id).isPresent();

            entity.setLifecycleFlags(GivenLifecycleFlags.archived());
            storeEntity(entity);

            assertFound(id).isEmpty();
        }

        @Test
        @DisplayName("deleted")
        void deleted() {
            E entity = createEntity(822);
            I id = entity.id();

            storeEntity(entity);

            assertFound(id).isPresent();

            entity.setLifecycleFlags(GivenLifecycleFlags.deleted());
            storeEntity(entity);

            assertFound(id).isEmpty();
        }

        private OptionalSubject assertFound(I id) {
            Optional<E> entity = repository().findActive(id);
            return Truth8.assertThat(entity);
        }
    }

    @Test
    @DisplayName("exclude non-active records from entity query")
    void excludeNonActiveRecords() {
        E activeEntity = createEntity(271);
        E archivedEntity = createEntity(42);
        E deletedEntity = createEntity(314);
        delete((TransactionalEntity) deletedEntity);
        archive((TransactionalEntity) archivedEntity);

        // Fill the storage
        storeEntity(activeEntity);
        storeEntity(archivedEntity);
        storeEntity(deletedEntity);

        Iterator<E> found = repository().find(emptyFilters(), emptyOrder(), emptyPagination(),
                                              emptyFieldMask());
        List<E> foundList = newArrayList(found);
        // Check results
        assertThat(foundList).hasSize(1);
        E actualEntity = foundList.get(0);
        assertEquals(activeEntity, actualEntity);
    }

    @Test
    @DisplayName("allow any lifecycle if column is involved in query")
    void ignoreLifecycleForColumns() {
        E activeEntity = createEntity(42);
        E archivedEntity = createEntity(314);
        E deletedEntity = createEntity(271);
        delete((TransactionalEntity) deletedEntity);
        archive((TransactionalEntity) archivedEntity);

        // Fill the storage
        storeEntity(activeEntity);
        storeEntity(archivedEntity);
        storeEntity(deletedEntity);

        CompositeFilter filter = all(eq(archived.name(), false));
        TargetFilters filters = TargetFiltersVBuilder
                .newBuilder()
                .addFilter(filter)
                .build();

        Iterator<E> found = repository().find(filters, emptyOrder(), emptyPagination(),
                                              emptyFieldMask());
        Collection<E> foundList = newArrayList(found);
        // Check result
        IterableSubject assertFoundList = assertThat(foundList);
        assertFoundList.hasSize(2);
        assertFoundList.containsExactly(activeEntity, deletedEntity);
    }

    @Test
    @DisplayName("cache entity columns on registration")
    void cacheColumnsOnRegister() {
        if (!repository().isRegistered()) {
            repository().onRegistered();
        }

        RecordStorage<I> storage = repository().recordStorage();
        EntityColumnCache entityColumnCache = storage.entityColumnCache();

        // Verify that cache contains searched column
        assertNotNull(entityColumnCache.findColumn("idString"));
    }
}
