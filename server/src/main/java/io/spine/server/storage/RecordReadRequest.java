/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A request to read a particular record from {@link RecordStorage}.
 *
 * <p>A result of this request is a record with the specified ID.
 *
 * <p>Two requests are considered equal if they have the same {@linkplain #getRecordId() record ID}.
 *
 * @param <I> the type of the target ID
 */
@Internal
public final class RecordReadRequest<I> implements ReadRequest<I> {

    private final I recordId;

    public RecordReadRequest(I recordId) {
        this.recordId = checkNotNull(recordId);
    }

    @Override
    public I getRecordId() {
        return recordId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        RecordReadRequest<?> that = (RecordReadRequest<?>) o;

        return recordId.equals(that.recordId);
    }

    @Override
    public int hashCode() {
        return recordId.hashCode();
    }
}
