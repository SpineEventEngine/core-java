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
import org.spine3.server.entity.status.EntityStatus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Meta-information that controls visibility of an entity.
 *
 * @author Alexander Yevsyukov
 */
public class Visibility<I> extends EntityMeta<I, EntityStatus> {

    public static <I> Visibility<I> of(I id) {
        return new Visibility<>(id);
    }

    public static <I> Visibility<I> of(Entity<I, ?, Visibility<I>> entity) {
        final Optional<Visibility<I>> metadata = entity.getMetadata();
        if (metadata.isPresent()) {
            return metadata.get();
        }
        // Not set before — initialize.
        final Visibility<I> result = new Visibility<>(entity.getId());
        entity.setMetadata(result);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @param id
     */
    protected Visibility(I id) {
        super(id);
    }

    public Visibility<I> setArchived(boolean archived) {
        final EntityStatus newStatus = getState().toBuilder()
                                                 .setArchived(archived)
                                                 .build();
        incrementState(newStatus);
        return this;
    }

    protected static <E extends EntityWithVisibility<?>> boolean isArchived(E entity) {
        checkNotNull(entity);
        final Optional<? extends Visibility<?>> visible = entity.getMetadata();
        return visible.isPresent() && visible.get()
                                             .getState()
                                             .getArchived();
    }

    protected static <E extends EntityWithVisibility<?>> boolean isDeleted(E entity) {
        checkNotNull(entity);
        final Optional<? extends Visibility<?>> visible = entity.getMetadata();
        return visible.isPresent() && visible.get()
                                             .getState()
                                             .getDeleted();
    }

    public Visibility<I> setDeleted(boolean deleted) {
        final EntityStatus newStatus = getState().toBuilder()
                .setDeleted(deleted)
                .build();
        incrementState(newStatus);
        return this;
    }
}
