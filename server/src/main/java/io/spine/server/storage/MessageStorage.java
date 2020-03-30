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
import io.spine.client.ResponseFormat;

import java.util.Iterator;

/**
 * An abstract base for storage implementations, which store the Protobuf messages as records.
 *
 * <p>Each stored message must be identified.
 *
 * <p>Additionally, some attributes may be stored along with the message itself
 * to allow further querying.
 */
@SPI
public abstract class MessageStorage<I, M extends Message> extends AbstractStorage<I, M> {

    private final Columns<M> columns;

    /**
     * Creates the new storage instance.
     *
     * @param columns
     *         definitions of the columns to store along with each message
     * @param multitenant
     *         whether this storage should support multi-tenancy
     */
    protected MessageStorage(Columns<M> columns, boolean multitenant) {
        super(multitenant);
        this.columns = columns;
    }

    /**
     * Writes the message as the storage record by the given identifier.
     *
     * @param id
     *         the ID for the record
     * @param record
     *         the record to store
     */
    @Override
    public final synchronized void write(I id, M record) {
        MessageWithColumns<I, M> withCols = MessageWithColumns.create(id, record, this.columns);
        write(withCols);
    }

    /**
     * Reads all message records according to the passed query.
     *
     * <p>The default {@link ResponseFormat} is used.
     *
     * @param query
     *         the query to execute
     * @return iterator over the matching messages
     */
    public final Iterator<M> readAll(MessageQuery<I> query) {
        return readAll(query, ResponseFormat.getDefaultInstance());
    }

    /**
     * Reads all message records in the storage.
     *
     * @return iterator over the records
     */
    public final Iterator<M> readAll() {
        return readAll(MessageQuery.all());
    }

    /**
     * Returns the definition of the columns to store along with each message record
     * in this storage.
     */
    @Internal
    public Columns<M> columns() {
        return columns;
    }

    /**
     * Reads all message records in this storage according to the passed response format.
     *
     * @param format
     *         the format of the response
     * @return iterator over the message records
     */
    public abstract Iterator<M> readAll(ResponseFormat format);

    /**
     * Reads the message records which match the passed query and returns the result
     * in the specified response format.
     *
     * @param query
     *         the query to execute
     * @param format
     *         format of the expected response
     * @return iterator over the matching message records
     */
    public abstract Iterator<M> readAll(MessageQuery<I> query, ResponseFormat format);

    /**
     * Writes the message along with its filled-in column values to the storage.
     *
     * @param record
     *         the message to write and additional columns with their values
     */
    @Internal
    public abstract void write(MessageWithColumns<I, M> record);

    /**
     * Writes the batch of the messages along with their filled-in columns to the storage.
     *
     * @param records
     *         messages and their column values
     */
    @Internal
    public abstract void writeAll(Iterable<MessageWithColumns<I, M>> records);

    /**
     * Physically deletes the message record from the storage by the record identifier.
     *
     * <p>In case the record with the specified identifier is not found in the storage,
     * this method does nothing and returns {@code false}.
     *
     * @param id
     *         identifier of the record to delete
     * @return {@code true} if the record was deleted,
     *         or {@code false} if the record with the specified identifier was not found
     */
    public abstract boolean delete(I id);

    /**
     * Deletes the batch of message records by their identifiers.
     *
     * <p>If for some provided identifiers there is no records in the storage, such identifiers
     * are silently skipped.
     *
     * @param ids
     *         identifiers of the records to delete
     */
    public abstract void deleteAll(Iterable<I> ids);
}
