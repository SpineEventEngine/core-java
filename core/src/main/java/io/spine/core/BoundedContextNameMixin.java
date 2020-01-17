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

package io.spine.core;

import io.spine.annotation.GeneratedMixin;
import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A generated mixin interface for {@link BoundedContextName}.
 */
@SuppressWarnings("ClassReferencesSubclass")
@GeneratedMixin
interface BoundedContextNameMixin extends BoundedContextNameOrBuilder {

    /**
     * Obtains the name of the Bounded Context.
     */
    default String value() {
        return getValue();
    }

    /**
     * Converts this name into the name of the associated System Bounded Context.
     *
     * <p>A name of a System context is the name of the domain context with the {@code _System}
     * suffix.
     */
    @Internal
    default BoundedContextName toSystem() {
        String value = value() + "_System";
        BoundedContextName result = BoundedContextName
                .newBuilder()
                .setValue(value)
                .build();
        return result;
    }

    /**
     * Compares the name of the system counterpart of the given context to this name.
     *
     * @param name
     *         the name of another context
     * @return {@code true} if this instance represents the name of the System context of the given
     *         domain context, {@code false} otherwise
     */
    default boolean isSystemOf(BoundedContextName name) {
        checkNotNull(name);
        return name.toSystem()
                   .equals(this);
    }
}
