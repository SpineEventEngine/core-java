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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Set;

/**
 * Abstract for builder of service classes.
 *
 * @param <T>
 *         the type of service to be produced.
 */
public abstract class AbstractServiceBuilder<T, B extends AbstractServiceBuilder<T, B>> {

    private final Set<BoundedContext> contexts = Sets.newHashSet();

    abstract B self();

    /**
     * Tells if this builder has no bounded context.
     */
    boolean isEmpty() {
        return contexts.isEmpty();
    }

    /**
     * Obtains immutable copy of already added contexts.
     */
    ImmutableSet<BoundedContext> contexts() {
        return ImmutableSet.copyOf(contexts);
    }

    /**
     * Adds the passed bounded context to be served by the query service.
     */
    @CanIgnoreReturnValue
    public B add(BoundedContext context) {
        contexts.add(context);
        return self();
    }

    /**
     * Excludes the passed bounded context from being served by the query service.
     */
    @CanIgnoreReturnValue
    public B remove(BoundedContext context) {
        contexts.remove(context);
        return self();
    }

    /**
     * Tells if the builder already contains the passed bounded context.
     */
    public boolean contains(BoundedContext context) {
        return contexts.contains(context);
    }

    /**
     * Creates a new instance of the service.
     */
    abstract T build();
}
