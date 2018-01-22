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
import io.spine.annotation.SPI;

import java.util.Iterator;

/**
 * The base interface for storages.
 *
 * @param <I> the type of identifiers
 * @param <M> the type of records
 * @param <R> the type of {@linkplain ReadRequest read requests}
 * @author Alexander Yevsyukov
 */
@SPI
public interface Storage<I, M extends Message, R extends ReadRequest<I>> extends AutoCloseable {

    /**
     * Verifies whether the storage is multitenant.
     *
     * @return {@code true} if the storage was created with multitenancy support,
     *         {@code false} otherwise
     */
    boolean isMultitenant();

    /**
     * Returns an iterator over identifiers of records in the storage.
     */
    Iterator<I> index();

    /**
     * Reads a record from the storage by the specified request.
     *
     * @param request the request to read the record
     * @return a record instance
     *         or {@code Optional.absent()} if there is no record matching this request
     * @throws IllegalStateException if the storage was closed before
     */
    Optional<M> read(R request);

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     the ID for the record
     * @param record a record to store
     * @throws IllegalStateException if the storage is closed
     */
    void write(I id, M record);

    /**
     * Closes the storage.
     *
     * <p>Implementations may throw specific exceptions.
     */
    @Override
    void close();

    /**
     * Verifies whether the storage is open.
     *
     * @return {@code true} if the storage is open, {@code false} otherwise
     */
    boolean isOpen();
}
