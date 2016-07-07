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
import org.spine3.SPI;

/**
 * Abstract base for storages.
 *
 * <p>A storage can read and write messages of the given type.
 *
 * @param <I> the type of IDs of storage records
 * @param <R> the type of records kept in the storage
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class AbstractStorage<I, R extends Message> implements Storage {

    private final boolean multitenant;
    private boolean open = true;

    protected AbstractStorage(boolean multitenant) {
        this.multitenant = multitenant;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMultitenant() {
        return multitenant;
    }

    /**
     * Reads a record from the storage by the passed ID.
     *
     * @param id the ID of the record to load
     * @return a record instance or the default record instance if there is no record with this ID
     * @throws IllegalStateException if the storage was closed before
     * @see Message#getDefaultInstanceForType()
     */
    public abstract R read(I id);

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id the ID for the record
     * @param record a record to store
     * @throws IllegalStateException if the storage is closed
     */
    public abstract void write(I id, R record);

    /**
     * Ensures the storage is not closed.
     *
     * <p>If the storage is closed throws {@code IllegalStateException} with the passed message.
     *
     * @param message exception message
     * @throws IllegalStateException if the storage is closed
     */
    protected void checkNotClosed(String message) throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Ensures the storage is not closed.
     *
     * <p>If the storage is closed throws {@code IllegalStateException}.
     *
     * @throws IllegalStateException if the storage is closed
     */
    protected void checkNotClosed() throws IllegalStateException {
        checkNotClosed("The storage is closed.");
    }

    /**
     * @return {@code true} if the storage is open for writing (wasn't closed until now), {@code false} otherwise
     * @see #close()
     */
    public boolean isOpen() {
        return this.open;
    }

    /**
     * @return {@code true} if the storage is closed, {@code false} otherwise
     * @see #close()
     */
    public boolean isClosed() {
        return !open;
    }

    /**
     * Closes the storage.
     *
     * @throws IllegalStateException if the storage was already closed
     */
    @Override
    public void close() throws Exception {
        checkNotClosed();
        this.open = false;
    }
}
