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

package org.spine3.server.storage;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.SPI;

import java.util.Iterator;

/**
 * The base interface for storages.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public interface Storage<I, R extends Message> extends AutoCloseable {

    /**
     * Verifies whether the storage is multitenant.
     *
     * <p>A multitenant storage should take into account a current tenant (obtained via
     * {@link org.spine3.server.users.CurrentTenant#get() CurrentTenant.get() })
     * when performing operations with the data it stores.
     *
     * @return {@code true} if the storage was created with multitenancy support,
     *          {@code false} otherwise
     * @see org.spine3.server.users.CurrentTenant#get() CurrentTenant.get()
     */
    boolean isMultitenant();

    /**
     * Returns an iterator over identifiers of records in the storage.
     */
    Iterator<I> index();

    /**
     * Reads a record from the storage by the passed ID.
     *
     * @param id the ID of the record to load
     * @return a record instance or {@code Optional.absent()} if there is no record with this ID
     * @throws IllegalStateException if the storage was closed before
     */
    Optional<R> read(I id);

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     the ID for the record
     * @param record a record to store
     * @throws IllegalStateException if the storage is closed
     */
    void write(I id, R record);
}
