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

package io.spine.server.storage;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.server.entity.LifecycleFlags;

/**
 * A storage that allows to update {@linkplain LifecycleFlags lifecycle flags} of entities.
 *
 * @author Alexander Yevsyukov
 */
public interface StorageWithLifecycleFlags<I, M extends Message, R extends ReadRequest<I>>
        extends Storage<I, M, R> {

    /**
     * Reads the visibility status for the entity with the passed ID.
     *
     * <p>This method returns {@code Optional.absent()} if none of the
     * flags of visibility flags were set before.
     *
     * @param id the ID of the entity
     * @return the aggregate visibility or {@code Optional.absent()}
     */
    Optional<LifecycleFlags> readLifecycleFlags(I id);

    /**
     * Writes the visibility status for the entity with the passed ID.
     *
     * @param id         the ID of the entity for which to update the status
     * @param flags the status to write
     */
    void writeLifecycleFlags(I id, LifecycleFlags flags);
}
