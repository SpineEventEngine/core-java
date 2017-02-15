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
 * An abstract base for business entities that can be made invisible to
 * general queries.
 *
 * <p>For example, a repository may not show entities that are marked as “archived”
 * or “deleted”.
 *
 * @author Alexander Yevsyukov
 */
public abstract class VisibleEntity<I, S extends Message> extends Entity<I, S, Visibility<I>> {

    /**
     * {@inheritDoc}
     */
    protected VisibleEntity(I id) {
        super(id);
    }

    /**
     * Ensures that the visibility metadata is set in the entity.
     *
     * <p>If the metadata was not set before, it is created in the default state
     * and set.
     *
     * @return an instance of entity metadata
     */
    protected Visibility<I> visibility() {
        final Optional<Visibility<I>> metadata = getMetadata();
        if (metadata.isPresent()) {
            return metadata.get();
        }

        // Not set before — initialize.
        final Visibility<I> result = new Visibility<>(getId());
        setMetadata(result);
        return result;
    }

    /**
     * Tests whether the entity is marked as archived.
     *
     * @return {@code true} if the entity is archived, {@code false} otherwise
     */
    protected boolean isArchived() {
        return Visibility.isArchived(this);
    }

    /**
     * Sets {@code archived} status flag to the passed value.
     */
    protected void setArchived(boolean archived) {
        visibility().setArchived(archived);
    }

    /**
     * Tests whether the entity is marked as deleted.
     *
     * @return {@code true} if the entity is deleted, {@code false} otherwise
     */
    protected boolean isDeleted() {
        return Visibility.isDeleted(this);
    }

    /**
     * Sets {@code deleted} status flag to the passed value.
     */
    protected void setDeleted(boolean deleted) {
        visibility().setDeleted(deleted);
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
