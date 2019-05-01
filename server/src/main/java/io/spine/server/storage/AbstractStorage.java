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

import com.google.protobuf.Message;

/**
 * Abstract base for storages.
 *
 * <p>A storage can read and write messages of the given type.
 *
 * @param <I> the type of IDs of storage records
 * @param <M> the type of records kept in the storage
 * @param <R> the type of {@linkplain ReadRequest read requests} for the storage
 */
public abstract class AbstractStorage<I, M extends Message, R extends ReadRequest<I>>
                implements Storage<I, M, R> {

    private final boolean multitenant;
    private boolean open = true;

    protected AbstractStorage(boolean multitenant) {
        this.multitenant = multitenant;
    }

    @Override
    public boolean isMultitenant() {
        return multitenant;
    }

    /**
     * Ensures the storage is not closed.
     *
     * <p>If the storage is closed, throws {@code IllegalStateException} with the passed message.
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
     * <p>If the storage is closed, throws {@code IllegalStateException}.
     *
     * @throws IllegalStateException if the storage is closed
     */
    protected void checkNotClosed() throws IllegalStateException {
        checkNotClosed("The storage is closed.");
    }

    /**
     * Tests whether the storage is open.
     *
     * @return {@code true} if the storage is open for writing (wasn't closed until now),
     *         {@code false} otherwise
     * @see #close()
     */
    @Override
    public boolean isOpen() {
        return this.open;
    }

    /**
     * Tests whether the storage is closed.
     *
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
    public void close() {
        checkNotClosed();
        this.open = false;
    }
}
