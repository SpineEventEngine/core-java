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
import com.google.protobuf.Empty;
import com.google.protobuf.Message;

import javax.annotation.Nullable;

/**
 * A metadata for an entity.
 *
 * @param <I> the type of entity ID. Must be the same type as in entities we annotate with metadata
 * @param <M> the type of the metadata state
 * @author Alexander Yevsyukov
 */
public abstract class EntityMeta<I, M extends Message> extends Entity<I, M, EntityMeta.Nothing<I>> {

    /**
     * {@inheritDoc}
     */
    protected EntityMeta(I id) {
        super(id);
    }

    /**
     * Always throws {@link UnsupportedOperationException} because
     * this class does not support metadata.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Optional<Nothing<I>> getMetadata() {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not have metadata.");
    }

    /**
     * Always throws {@link UnsupportedOperationException} because
     * this class does not support metadata.
     */
    @SuppressWarnings({"MethodDoesntCallSuperMethod", "ClassReferencesSubclass"}) // to finish the recursion
    @Override
    void setMetadata(@Nullable Nothing<I> metadata) {
        throw unsupported();
    }

    public static final class Nothing<I> extends EntityMeta<I, Empty> {
        private Nothing(I id) {
            super(id);
        }
    }
}

