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
import org.spine3.base.Stringifiers;
import org.spine3.server.entity.status.CannotModifyArchivedEntity;
import org.spine3.server.entity.status.CannotModifyDeletedEntity;
import org.spine3.server.entity.status.EntityStatus;

/**
 *
 * @author Alexander Yevsyukov
 */
public abstract class EntityWithStatus<I, S extends Message> extends Entity<I, S, EntityStatus> {

    /**
     * {@inheritDoc}
     */
    protected EntityWithStatus(I id) {
        super(id);
    }

    protected EntityStatus getEntityStatus() {
        final Optional<EntityStatus> status = getMetadata();
        if (status.isPresent()) {
            return status.get();
        }
        return EntityStatus.getDefaultInstance();
    }

    /**
     * Tests whether the entity is marked as archived.
     *
     * @return {@code true} if the entity is archived, {@code false} otherwise
     */
    protected boolean isArchived() {
        return getEntityStatus().getArchived();
    }

    /**
     * Sets {@code archived} status flag to the passed value.
     */
    protected void setArchived(boolean archived) {
        final EntityStatus newStatus = getEntityStatus().toBuilder()
                                                        .setArchived(archived)
                                                        .build();
        setMetadata(newStatus);
    }

    /**
     * Tests whether the entity is marked as deleted.
     *
     * @return {@code true} if the entity is deleted, {@code false} otherwise
     */
    protected boolean isDeleted() {
        return getEntityStatus().getDeleted();
    }

    /**
     * Sets {@code deleted} status flag to the passed value.
     */
    protected void setDeleted(boolean deleted) {
        final EntityStatus newStatus = getEntityStatus().toBuilder()
                                                        .setDeleted(deleted)
                                                        .build();
        setMetadata(newStatus);
    }

    /**
     * Ensures that the entity is not marked as {@code archived}.
     *
     * @throws CannotModifyArchivedEntity if the entity in in the archived status
     * @see #getMetadata()
     * @see EntityStatus#getArchived()
     */
    protected void checkNotArchived() throws CannotModifyArchivedEntity {
        if (isArchived()) {
            final String idStr = Stringifiers.idToString(getId());
            throw new CannotModifyArchivedEntity(idStr);
        }
    }

    /**
     * Ensures that the entity is not marked as {@code deleted}.
     *
     * @throws CannotModifyDeletedEntity if the entity is marked as {@code deleted}
     * @see #getMetadata()
     * @see EntityStatus#getDeleted()
     */
    protected void checkNotDeleted() throws CannotModifyDeletedEntity {
        if (isDeleted()) {
            final String idStr = Stringifiers.idToString(getId());
            throw new CannotModifyDeletedEntity(idStr);
        }
    }
}
