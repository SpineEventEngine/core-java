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

import com.google.common.collect.FluentIterable;
import com.google.protobuf.Message;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.users.TenantId;

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

    public ReadFunction<I, E, S> loadOrCreate(TenantId tenantId) {
        return new LoadOrCreate<>(tenantId, storageIO, converter);
    }

    public RecordStorageIO.Write<I> write(TenantId tenantId) {
        return storageIO.write(tenantId);
    }

    public interface ReadFunction<I, E extends Entity<I, S>, S extends Message>
            extends SerializableFunction<I, E> {

        EntityStorageConverter<I, E, S> getConverter();
    }

    private static class LoadOrCreate<I, E extends Entity<I, S>, S extends Message>
            implements ReadFunction<I, E, S> {

        private static final long serialVersionUID = 0L;
        private final RecordStorageIO.FindById<I> queryFn;
        private final EntityStorageConverter<I, E, S> converter;

        private LoadOrCreate(TenantId tenantId,
                             RecordStorageIO<I> storageIO,
                             EntityStorageConverter<I, E, S> converter) {
            this.queryFn = storageIO.findFn(tenantId);
            this.converter = converter;
        }

        @Override
        public E apply(I input) {
            //TODO:2017-05-25:alexander.yevsyukov: Load only one record by ID.

            final Iterable<EntityRecord> queryResult = null; // queryFn.apply(singleRecord(input));
            final EntityRecord record = FluentIterable.from(queryResult)
                                                      .get(0);
            final E result = converter.reverse()
                                      .convert(record);
            return result;
        }

        @Override
        public EntityStorageConverter<I, E, S> getConverter() {
            return converter;
        }
    }
}
