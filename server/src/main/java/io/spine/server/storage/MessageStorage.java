/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;

import java.util.List;

import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;

/**
 * A storage which allows persisting the {@link Message}s as storage records.
 *
 * <p>The {@linkplain #recordSpec() record specification} is used to determine the record identifier
 * and the columns.
 *
 * <p>To persist the {@link io.spine.server.entity.Entity Entity} data,
 * see {@link io.spine.server.entity.storage.EntityRecordStorage EntityRecordStorage}, which
 * uses not only the {@code Entity} state, but the {@code Entity} lifecycle attributes to prepare
 * the storage record.
 *
 * @implNote This storage delegates all the operations to the underlying
 *         {@link RecordStorage}, which is configured according to the record specification
 *         for the persisted {@code Message}s
 * @see io.spine.server.entity.storage.EntityRecordStorage EntityRecordStorage
 */
@SPI
public abstract class MessageStorage<I, M extends Message> extends RecordStorageDelegate<I, M> {

    protected MessageStorage(RecordStorage<I, M> delegate) {
        super(delegate);
    }

    /**
     * Writes the message to the storage.
     *
     * <p>The identifier and the columns for the written record are extracted via
     * the {@linkplain #recordSpec() record specification}.
     *
     * @param message
     *         the message to write
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    protected void write(M message) {
        RecordWithColumns<I, M> withCols = toRecord(message);
        super.write(withCols);
    }

    /**
     * Writes the given message under the given identifier.
     *
     * <p>The columns are extracted using the {@linkplain #recordSpec() record specification}.
     *
     * <p>The identifier for the written record is determined by the passed {@code id} value,
     * while the ID value provided record specification is ignored.
     *
     * @param id
     *         the identifier to use for the written record
     * @param message
     *         the message to write
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public synchronized void write(I id, M message) {
        RecordWithColumns<I, M> record = RecordWithColumns.create(id, message, recordSpec());
        super.write(record);
    }

    /**
     * Writes the batch of messages to the storage.
     *
     * <p>The identifier and the columns for the written records are extracted via
     * the {@linkplain #recordSpec() record specification}.
     *
     * @param messages
     *         the batch of the messages to write
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    protected void writeBatch(Iterable<M> messages) {
        List<RecordWithColumns<I, M>> records = stream(messages).map(this::toRecord)
                                                                .collect(toList());
        super.writeAll(records);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for the type covariance.
     */
    @Internal
    @Override
    @SuppressWarnings("unchecked")  // Columns of the stored messages are taken from the messages.
    protected RecordSpec<I, M, M> recordSpec() {
        return (RecordSpec<I, M, M>) super.recordSpec();
    }

    /**
     * Extracts the identifier and the columns from the given message, creating
     * a new {@code RecordWithColumns}.
     */
    private RecordWithColumns<I, M> toRecord(M message) {
        return RecordWithColumns.create(message, recordSpec());
    }
}
