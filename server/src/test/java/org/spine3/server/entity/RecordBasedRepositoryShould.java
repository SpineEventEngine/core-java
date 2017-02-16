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

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.test.Tests;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordBasedRepositoryShould<E extends AbstractEntity<I, S>, I, S extends Message> {

    @SuppressWarnings("ProtectedField") // we use the reference in the derived test cases.
    protected RecordBasedRepository<I, E, S> repository;

    protected abstract RecordBasedRepository<I, E, S> createRepository();

    protected abstract E createEntity();

    protected abstract List<E> createEntities(int count);

    protected abstract I createId(int value);

    @Before
    public void initRepository() {
        this.repository = createRepository();
    }

    private List<E> createAndStoreEntities(RecordBasedRepository<I, E, S> repo, int count) {
        final List<E> entities = createEntities(count);

        for (E entity : entities) {
            repo.store(entity);
        }
        return entities;
    }

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

        repository.store(entity);

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We're sure as we just stored the entity.
        final VersionableEntity<?,?> found = repository.load(entity.getId()).get();

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

        final Collection<E> found = repository.loadAll(ids);

        assertSize(ids.size(), found);

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void find_all_entities() {
        final List<E> entities = createAndStoreEntities(repository, 150);
        final Collection<E> found = repository.loadAll();
        assertSize(entities.size(), found);

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @Test
    public void find_no_entities_if_empty() {
        final Collection<E> found = repository.loadAll();
        assertSize(0, found);
    }

    @Test
    public void create_entity_on_loadOrCreate_if_not_found() {
        final int count = 3;
        //noinspection ResultOfMethodCallIgnored
        createAndStoreEntities(repository, count);

        I id = createId(count + 1);
        final E entity = repository.loadOrCreate(id);

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
        final VersionableEntity<I,S> sideEntity = createEntity();
        ids.add(sideEntity.getId());

        final Collection<E> found = repository.loadAll(ids);
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
        final Iterable<E> readEntities = repository.find(filters, firstFieldOnly);

        assertSize(ids.size(), readEntities);

        for (E entity : readEntities) {
            assertMatches(entity, firstFieldOnly);
        }
    }

    private static <E extends AbstractEntity<?, ?>> void assertMatches(E entity, FieldMask fieldMask) {
        final Message state = entity.getState();
        Tests.assertMatchesMask(state, fieldMask);
    }

    @Test
    public void mark_records_archived() {
        final E entity = createEntity();
        final I id = entity.getId();

        repository.store(entity);

        assertTrue(repository.load(id).isPresent());

        final boolean successful = repository.markArchived(id);

        assertTrue(successful);
        assertFalse(repository.load(id).isPresent());
    }

    @Test
    public void mark_records_deleted() {
        final E entity = createEntity();
        final I id = entity.getId();

        repository.store(entity);

        assertTrue(repository.load(id).isPresent());

        final boolean successful = repository.markDeleted(id);

        assertTrue(successful);
        assertFalse(repository.load(id).isPresent());
    }
}
