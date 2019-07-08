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

package io.spine.server;

import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkState;

/**
 * An integral part of a Bounded Context which is aware of the other parts.
 */
@Internal
public interface ContextAware {

    /**
     * Initializes this instance as a part of the given Bounded Context.
     *
     * <p>This method should be only called once. However, it is allowed to initialize a single
     * instance many times with the <strong>same</strong> Bounded Context.
     *
     * @param context the Context to which this instance belongs
     */
    void initialize(BoundedContext context);

    /**
     * Determines if this instance is already initialized with a Bounded Context.
     */
    boolean isInitialized();

    /**
     * Verifies that this instance is already initialized.
     *
     * <p>Throws an {@code IllegalStateException} if not initialized.
     */
    default void checkInitialized() {
        checkState(isInitialized(), "%s is NOT initialized.", this);
    }

    /**
     * Verifies that this instance is NOT initialized yet.
     *
     * <p>Throws an {@code IllegalStateException} if already initialized.
     */
    default void checkNotInitialized() {
        checkState(!isInitialized(), "%s is already initialized.", this);
    }
}
