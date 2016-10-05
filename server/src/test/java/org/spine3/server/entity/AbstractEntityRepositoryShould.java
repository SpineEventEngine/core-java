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

package org.spine3.server.entity;

import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.protobuf.AnyPacker;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public abstract class AbstractEntityRepositoryShould<E extends Entity<?, ?>> {

    @SuppressWarnings("unchecked")
    @Test
    public void find_single_entity_by_id() {
        final EntityRepository repo = repository();

        final E entity = entity();

        repo.store(entity);

        final Entity<?, ?> found = repo.load(entity.getId());

        assertEquals(found, entity);
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "unchecked"})
    @Test
    public void find_multiple_entities_by_ids() {
        final EntityRepository<?, E, ?> repo = repository();

        final int count = 10;
        final List<E> entities = entities(count);

        for (E entity : entities) {
            repo.store(entity);
        }

        final List ids = new LinkedList<>();
        for (int i = 0; i < count / 2; i++) {
            ids.add(entities.get(i).getId());
        }

        final Collection<E> found = repo.loadAll(ids);

        assertSize(ids.size(), found);

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "unchecked"})
    @Test
    public void handle_wrong_passed_ids() {
        final EntityRepository<?, E, ?> repo = repository();

        final int count = 10;
        final List<E> entities = entities(count);

        for (E entity : entities) {
            repo.store(entity);
        }

        final List ids = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            ids.add(entities.get(i).getId());
        }

        final Entity<?, ?> sideEntity = entity();

        ids.add(sideEntity.getId());

        final Collection<E> found = repo.loadAll(ids);

        assertSize(ids.size() - 1, found); // Check we've found all existing items

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "unchecked"})
    @Test
    public void retrieve_all_records_with_entity_filters_and_field_mask_applied() {
        final EntityRepository<?, E, ?> repo = repository();

        final int count = 10;
        final List<E> entities = entities(count);

        for (E entity : entities) {
            repo.store(entity);
        }

        final List<EntityId> ids = new LinkedList<>();
        for (int i = 0; i < count / 2; i++) {
            final EntityId id = EntityId.newBuilder()
                                        .setId(AnyPacker.pack((Message) entities.get(i)
                                                                                .getId()))
                                        .build();
            ids.add(id);
        }

        final EntityIdFilter filter = EntityIdFilter.newBuilder()
                                                    .addAllIds(ids)
                                                    .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(filter)
                                                   .build();

        final String firstFieldName = entities.get(0)
                                              .getState()
                                              .getDescriptorForType()
                                              .getFields()
                                              .get(0)
                                              .getFullName();

        final FieldMask firstFieldOnly = FieldMask.newBuilder()
                                                  .addPaths(firstFieldName)
                                                  .build();

        final Iterable<E> readEntities = repo.find(filters, firstFieldOnly);

        assertSize(ids.size(), readEntities);

        for (E entity : readEntities) {
            assertMatches(entity, firstFieldOnly);
        }
    }

    private void assertMatches(E entity, FieldMask fieldMask) {
        final Message state = entity.getState();
        final List<String> paths = fieldMask.getPathsList();

        for (Descriptors.FieldDescriptor field : state.getDescriptorForType()
                                                      .getFields()) {
            if (field.isRepeated()) {
                continue;
            }

            assertEquals(state.hasField(field), paths.contains(field.getFullName()));
        }
    }

    protected abstract EntityRepository<?, E, ?> repository();

    protected abstract E entity();

    protected abstract List<E> entities(int count);
}
