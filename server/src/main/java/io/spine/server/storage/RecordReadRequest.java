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

package io.spine.server.storage;

import com.google.protobuf.FieldMask;
import io.spine.annotation.SPI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A read request for {@link RecordStorage}.
 *
 * @param <I> the type of the target ID
 * @author Dmytro Grankin
 */
@SPI
public final class RecordReadRequest<I> implements ReadRequest<I> {

    private final I id;
    private final FieldMask fieldMask;

    public RecordReadRequest(I id, FieldMask fieldMask) {
        this.id = checkNotNull(id);
        this.fieldMask = checkNotNull(fieldMask);
    }

    @Override
    public I getId() {
        return id;
    }

    /**
     * Obtains the field mask to apply on the record with the {@linkplain #getId() ID}.
     *
     * @return the field mask for the requested record
     */
    public FieldMask getFieldMask() {
        return fieldMask;
    }
}
