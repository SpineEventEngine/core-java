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

import org.spine3.SPI;

/**
 * Abstract base for storages.
 *
 * @author Alexander Yevsyukov
 */
@SPI
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class AbstractStorage implements AutoCloseable {

    private boolean open = true;

    protected void checkNotClosed(String message) throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException(message);
        }
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
        checkNotClosed("Already closed.");
        this.open = false;
    }
}
