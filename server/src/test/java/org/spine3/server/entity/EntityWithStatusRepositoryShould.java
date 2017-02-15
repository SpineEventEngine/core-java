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
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.server.entity.status.EntityStatus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public abstract class EntityWithStatusRepositoryShould<E extends EntityWithStatus<I, S>, I, S extends Message>
            extends RecordBasedRepositoryShould<E, I, S, EntityStatus> {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void mark_records_archived() {
        final E entity = createEntity();
        final I id = entity.getId();

        repository.store(entity);

        final Optional<E> loaded = repository.load(id);
        assertTrue(loaded.isPresent());

        repository.updateMetadata(id, loaded.get()
                                            .getEntityStatus()
                                            .toBuilder()
                                            .setArchived(true)
                                            .build());
        assertFalse(repository.load(id).isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void mark_records_deleted() {
        final E entity = createEntity();
        final I id = entity.getId();

        repository.store(entity);

        final Optional<E> loaded = repository.load(id);
        assertTrue(loaded.isPresent());

        repository.updateMetadata(id, loaded.get()
                                            .getEntityStatus()
                                            .toBuilder()
                                            .setDeleted(true)
                                            .build());

        assertFalse(repository.load(id).isPresent());
    }
}
