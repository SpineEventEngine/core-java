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

import org.junit.Test;

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

        final Entity<?, ?> found = repo.find(entity.getId());

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

        final Collection<E> found = repo.findBulk(ids);

        assertSize(count / 2, found);

        for (E entity : found) {
            assertContains(entity, entities);
        }
    }

    protected abstract EntityRepository<?, E, ?> repository();

    protected abstract E entity();

    protected abstract List<E> entities(int count);
}
