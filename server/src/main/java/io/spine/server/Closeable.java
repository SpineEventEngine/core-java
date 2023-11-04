/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base interface for server-side objects that may hold resources that need to be released
 * at the end of the lifecycle of the object.
 *
 * <p>A class will benefit from implementing <em>this</em> interface instead of
 * {@link AutoCloseable} if it needs to see if the instance is {@linkplain #isOpen() open}
 * prior to making other calls.
 *
 * @see #isOpen()
 * @see #checkOpen()
 */
public interface Closeable extends AutoCloseable {

    /**
     * Tells if the object is still open.
     *
     * <p>Implementations must return {@code false} after {@link #close()} is invoked.
     */
    boolean isOpen();

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to remove the checked exception from the signature.
     */
    @Override
    void close();

    /**
     * Ensures that the object is {@linkplain #isOpen() open}.
     *
     * @throws IllegalStateException otherwise
     */
    default void checkOpen() throws IllegalStateException {
        checkState(isOpen(), "`%s` is already closed.", this);
    }

    /**
     * Performs the release of the resources held by this object only if it is still open.
     * Otherwise, does nothing.
     *
     * @throws IllegalStateException
     *          if an exception occurs on {@link #close()}
     */
    default void closeIfOpen() {
        if (isOpen()) {
            close();
        }
    }
}
