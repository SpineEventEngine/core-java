/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.storage;

import com.google.protobuf.Message;

import javax.annotation.CheckReturnValue;
import java.util.Map;

/**
 * Mixin contract for storages providing bulk operations.
 *
 * <p>Defines the common API for storages, which are able to effectively implement bulk reads and writes.
 *
 * @param <I> a type for entity identifiers
 * @param <R> stored record type
 * @author Alex Tymchenko
 */
interface BulkStorageOperationsMixin<I, R extends Message> {

    /**
     * Reads the records from the storage with the given IDs.
     *
     * <p>The size of {@link Iterable} returned is always the same as the size of given IDs.
     *
     * <p>In case there is no record for a particular ID, {@code null} will be present in the result instead.
     * In this way {@code readMultiple()} callees are able to track the absenсe of a certain element by comparing
     * the input IDs and resulting {@code Iterable}.
     *
     * <p>E.g. {@code readMultiple( Lists.newArrayList(idPresentInStorage, idNonPresentInStorage) )} will return
     * an {@code Iterable} with two elements, first of which is non-null and the second is null.
     *
     * @param ids record IDs of interest
     * @return the {@link Iterable} containing the records matching the given IDs
     * @throws IllegalStateException if the storage was closed before
     */
    @CheckReturnValue
    Iterable<R> readMultiple(Iterable<I> ids);

    /**
     * Reads all the records from the storage.
     *
     * <p>Each record is returned as a {@link Map} of record ID to the instance of a record.
     * Such an approach enables turning the records into entities for callees.
     *
     * @return the {@code Map} containing the ID -> record entries.
     * @throws IllegalStateException if the storage was closed before
     */
    @CheckReturnValue
    Map<I, R> readAll();
}
