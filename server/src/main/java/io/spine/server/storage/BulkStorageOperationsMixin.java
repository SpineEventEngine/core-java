/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import java.util.Iterator;

/**
 * Mixin contract for storages providing bulk operations.
 *
 * <p>Defines the common API for storages, which are able to effectively implement
 * bulk reads and writes.
 *
 * @param <I>
 *         a type for entity identifiers
 * @param <R>
 *         stored record type
 * @author Alex Tymchenko
 */
interface BulkStorageOperationsMixin<I, R extends Message> {

    /**
     * Reads the active records with the given IDs from the storage.
     *
     * <p>The size of the returned {@code Iterator} matches the size
     * of the given IDs {@code Iterable}.
     *
     * <p>In case there is no record for a particular ID, {@code null} will be present
     * in the result. In this way {@code readMultiple()} callers are able to track
     * the absence of a certain element by comparing the input IDs and resulting {@code Iterable}.
     *
     * <p>E.g. {@code readMultiple(Lists.newArrayList(idPresentInStorage, idNonPresentInStorage, 
     * idPresentForInactiveEntity))} will return an {@code Iterable} with three elements,
     * first of which is non-{@code null} and the other two are {@code null}.
     * 
     * @param ids
     *         IDs of record of interest
     * @return an {@link Iterator} of nullable messages
     * @throws IllegalStateException
     *         if the storage was closed before finishing
     */
    Iterator<R> readMultiple(Iterable<I> ids);

    /**
     * Reads all the active records from the storage.
     * 
     * @return the {@link Iterator} containing the ID - record entries.
     * @throws IllegalStateException
     *         if the storage was closed before finishing
     */
    Iterator<R> readAll();
}
