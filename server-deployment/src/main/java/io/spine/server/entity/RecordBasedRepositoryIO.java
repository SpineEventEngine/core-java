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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.server.storage.RecordStorageIO;
import io.spine.users.TenantId;

/**
 * Beam I/O operations for a {@link RecordBasedRepository}.
 *
 * @author Alexander Yevsyukov
 */
public class RecordBasedRepositoryIO<I, E extends Entity<I, S>, S extends Message> {

    private final RecordStorageIO<I> storageIO;
    private final EntityStorageConverter<I, E, S> converter;

    protected RecordBasedRepositoryIO(RecordStorageIO<I> storageIO,
                                      EntityStorageConverter<I, E, S> converter) {
        this.storageIO = storageIO;
        this.converter = converter;
    }

    protected RecordStorageIO<I> storageIO() {
        return storageIO;
    }

    public EntityStorageConverter<I, E, S> getConverter() {
        return converter;
    }

    public RecordStorageIO.Write<I> write(TenantId tenantId) {
        return storageIO.write(tenantId);
    }

    public RecordStorageIO.ReadFn<I> read(TenantId tenantId) {
        return storageIO.readFn(tenantId);
    }
}
