/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.model.ModelTests;
import io.spine.server.storage.RecordStorage;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.test.Tests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.core.given.GivenTenantId.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.TestTransaction.archive;
import static io.spine.server.entity.TestTransaction.delete;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.Verify.assertContains;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.test.Verify.assertNotContains;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The abstract test for the {@linkplain RecordBasedRepository} derived classes.
 *
 * @param <E> the type of the {@link Entity} of this repository; the type is checked to implement
 *            {@link TestEntityWithStringColumn} at runtime
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("ConstantConditions")
public abstract class RecordBasedRepositoryShould<E extends AbstractVersionableEntity<I, S>,
        I,
        S extends Message>
        extends TenantAwareTest {

    @SuppressWarnings("ProtectedField") // we use the reference in the derived test cases.
    protected RecordBasedRepository<I, E, S> repository;

    protected abstract RecordBasedRepository<I, E, S> createRepository();

    protected abstract E createEntity();

    protected abstract List<E> createEntities(int count);

    protected abstract I createId(int value);

    @Before
    public void setUp() {
        ModelTests.clearModel();
        this.repository = createRepository();
        setCurrentTenant(newUuid());
    }

    @After
    public void tearDown() throws Exception {
        clearCurrentTenant();
    }

    /*
     * Store/load functions for working in multi-tenant execution context
     **********************************************************************/

    private void storeEntity(final E entity) {
        repository.store(entity);
    }

    private List<E> createAndStoreEntities(final RecordBasedRepository<I, E, S> repo, int count) {
        final List<E> entities = createEntities(count);
        for (E entity : entities) {
            repo.store(entity);
        }
        return entities;
    }

    private Optional<E> find(final I id) {
        return repository.find(id);
    }

    private Iterator<E> loadMany(final List<I> ids) {
        return repository.loadAll(ids);
    }

    private Iterator<E> loadAll() {
        return repository.loadAll();
    }

    private E loadOrCreate(final I id) {
        return repository.findOrCreate(id);
    }

    private Iterator<E> find(EntityFilters filters,
                             FieldMask firstFieldOnly) {
        return repository.find(filters, firstFieldOnly);
    }

    /*
     * Tests
     ************/

    @Test
    public void create_entities() {
        final I id = createId(5);
        final E projectEntity = repository.create(id);
        assertNotNull(projectEntity);
        assertEquals(id, projectEntity.getId());
    }

    @Test
    public void find_single_entity_by_id() {
        final E entity = createEntity();

        storeEntity(entity);

        final Entity<?, ?> found = find(entity.getId()).get();

        assertEquals(found, entity);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void find_multiple_entities_by_ids() {
        final int count = 10;
        final List<E> entities = createAndStoreEntities(repository, count);

        final List<I> ids = Lists.newLinkedList();

        // Find some of the records (half of them in this case)
        for (int i = 0; i < count / 2; i++) {
            ids.add(entities.get(i)
                            .getId());
        }

        final Collection<E> found = newArrayList(loadMany(ids));

        assertSize(ids.size(), found);

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void find_all_entities() {
        final List<E> entities = createAndStoreEntities(repository, 150);
        final Collection<E> found = newArrayList(loadAll());
        assertSize(entities.size(), found);

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @Test
    public void find_entities_by_query() {
        final I id1 = createId(271);
        final I id2 = createId(314);
        final Class<E> entityClass = repository.getEntityClass();
        final E entity1 = Given.entityOfClass(entityClass)
                               .withId(id1)
                               .build();
        final E entity2 = Given.entityOfClass(entityClass)
                               .withId(id2)
                               .build();
        repository.store(entity1);
        repository.store(entity2);

        final String fieldPath = "idString";
        final StringValue fieldValue = StringValue.newBuilder()
                                                  .setValue(id1.toString())
                                                  .build();
        final ColumnFilter filter = eq(fieldPath, fieldValue);
        final CompositeColumnFilter aggregatingFilter = CompositeColumnFilter.newBuilder()
                                                                             .addFilter(filter)
                                                                             .setOperator(ALL)
                                                                             .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(aggregatingFilter)
                                                   .build();
        final Collection<E> found = newArrayList(repository.find(filters,
                                                                 FieldMask.getDefaultInstance()));
        assertSize(1, found);
        assertContains(entity1, found);
        assertNotContains(entity2, found);
    }

    @Test
    public void find_no_entities_if_empty() {
        final Collection<E> found = newArrayList(loadAll());
        assertSize(0, found);
    }

    @Test
    public void create_entity_on_loadOrCreate_if_not_found() {
        final int count = 3;
        createAndStoreEntities(repository, count);

        final I id = createId(count + 1);
        final E entity = loadOrCreate(id);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void handle_wrong_passed_ids() {
        final int count = 10;
        final List<E> entities = createAndStoreEntities(repository, count);
        final List<I> ids = Lists.newLinkedList();
        for (int i = 0; i < count; i++) {
            ids.add(entities.get(i)
                            .getId());
        }
        final Entity<I, S> sideEntity = createEntity();
        ids.add(sideEntity.getId());

        final Collection<E> found = newArrayList(loadMany(ids));
        assertSize(ids.size() - 1, found); // Check we've found all existing items

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void retrieve_all_records_with_entity_filters_and_field_mask_applied() {
        final int count = 10;
        final List<E> entities = createAndStoreEntities(repository, count);
        final List<EntityId> ids = Lists.newLinkedList();

        // Find some of the records (half of them in this case)
        for (int i = 0; i < count / 2; i++) {
            final Message entityId = (Message) entities.get(i)
                                                       .getId();
            final EntityId id = EntityId.newBuilder()
                                        .setId(pack(entityId))
                                        .build();
            ids.add(id);
        }

        final EntityIdFilter filter = EntityIdFilter.newBuilder()
                                                    .addAllIds(ids)
                                                    .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(filter)
                                                   .build();
        final Descriptors.Descriptor entityDescriptor = entities.get(0)
                                                                .getState()
                                                                .getDescriptorForType();
        final FieldMask firstFieldOnly = FieldMasks.maskOf(entityDescriptor, 1);
        final Iterator<E> readEntities = find(filters, firstFieldOnly);
        final Collection<E> foundList = newArrayList(readEntities);

        assertSize(ids.size(), foundList);

        for (E entity : foundList) {
            assertMatches(entity, firstFieldOnly);
        }
    }

    private static <E extends AbstractVersionableEntity<?, ?>>
    void assertMatches(E entity, FieldMask fieldMask) {
        final Message state = entity.getState();
        Tests.assertMatchesMask(state, fieldMask);
    }

    @Test
    public void mark_records_archived() {
        final E entity = createEntity();
        final I id = entity.getId();

        storeEntity(entity);

        assertTrue(find(id).isPresent());

        entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                               .setArchived(true)
                                               .build());
        storeEntity(entity);

        assertFalse(find(id).isPresent());
    }

    @Test
    public void mark_records_deleted() {
        final E entity = createEntity();
        final I id = entity.getId();

        storeEntity(entity);

        assertTrue(find(id).isPresent());

        entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                               .setDeleted(true)
                                               .build());
        storeEntity(entity);

        assertFalse(find(id).isPresent());
    }

    @Test
    public void exclude_non_active_records_from_entity_query() {
        final I archivedId = createId(42);
        final I deletedId = createId(314);
        final I activeId = createId(271);

        final E activeEntity = repository.create(activeId);
        final E archivedEntity = repository.create(archivedId);
        final E deletedEntity = repository.create(deletedId);
        delete((EventPlayingEntity) deletedEntity);
        archive((EventPlayingEntity) archivedEntity);

        // Fill the storage
        repository.store(activeEntity);
        repository.store(archivedEntity);
        repository.store(deletedEntity);

        final Iterator<E> found = repository.find(EntityFilters.getDefaultInstance(),
                                                  FieldMask.getDefaultInstance());
        final List<E> foundList = newArrayList(found);
        // Check results
        assertSize(1, foundList);
        final E actualEntity = foundList.get(0);
        assertEquals(activeEntity, actualEntity);
    }

    @Test
    public void allow_any_lifecycle_if_column_involved() {
        final I archivedId = createId(42);
        final I deletedId = createId(314);
        final I activeId = createId(271);

        final E activeEntity = repository.create(activeId);
        final E archivedEntity = repository.create(archivedId);
        final E deletedEntity = repository.create(deletedId);
        delete((EventPlayingEntity) deletedEntity);
        archive((EventPlayingEntity) archivedEntity);

        // Fill the storage
        repository.store(activeEntity);
        repository.store(archivedEntity);
        repository.store(deletedEntity);

        final CompositeColumnFilter columnFilter = all(eq(archived.name(), false));
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(columnFilter)
                                                   .build();

        final Iterator<E> found = repository.find(filters, FieldMask.getDefaultInstance());
        final Collection<E> foundList = newArrayList(found);
        // Check result
        assertSize(2, foundList);
        assertContainsAll(foundList, activeEntity, deletedEntity);
    }

    @Test
    public void cache_entity_columns_on_registration() {
        if(!repository.isRegistered()) {
            repository.onRegistered();
        }

        final RecordStorage<I> storage = repository.recordStorage();
        final EntityColumnCache entityColumnCache = storage.getEntityColumnCache();

        // Verify that cache contains searched column
        entityColumnCache.findColumn("idString");
    }
}
