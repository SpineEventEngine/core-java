/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.GeneratedMixin;
import io.spine.type.KnownTypes;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkState;

/**
 * Useful methods for {@code Query}.
 */
@Immutable
@GeneratedMixin
public interface QueryMixin extends QueryOrBuilder {

    /**
     * Obtains the URL of the query target type.
     *
     * @throws IllegalStateException
     *         if the {@code Target} type is unknown to the application.
     */
    default TypeUrl targetType() {
        TypeUrl typeUrl = getTarget().type();
        checkState(KnownTypes.instance()
                             .contains(typeUrl),
                   "Unknown type URL: `%s`.", typeUrl.value());
        return typeUrl;
    }

    /**
     * Obtains target filters specified in the query.
     */
    default TargetFilters filters() {
        return getTarget().getFilters();
    }

    /**
     * Obtains the format for the response to the query.
     */
    default ResponseFormat responseFormat() {
        return getFormat();
    }

    /**
     * Tells if this query requests all instances of the {@linkplain #targetType() target type}.
     */
    default boolean all() {
        return getTarget().getIncludeAll();
    }
}
