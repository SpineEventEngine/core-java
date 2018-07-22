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
package io.spine.server.model;

import io.spine.annotation.Internal;

/**
 * Meta-data set to a {@link AbstractHandlerMethod HandlerMethod}.
 *
 * <p>Typical way to add more semantics to a method is via a parameterized annotation,
 * such as {@link io.spine.core.Subscribe Subscribe}.
 *
 * @author Alex Tymchenko
 */
@Internal
public interface MethodAttribute<V> {

    /**
     * An attribute name.
     *
     * @return a name of the attribute as {@code String}
     */
    String getName();

    /**
     * A value of the attribute.
     *
     * <p>As it'd typically be an annotation field value, its type cannot be restricted
     * to anything except for {@link Object}.
     *
     * @return the value of the attribute
     */
    V getValue();
}
