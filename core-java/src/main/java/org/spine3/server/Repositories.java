/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.protobuf.Message;

/**
 * Utilities for working with repositories.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Repositories {

    public static final int AGGREGATE_ID_CLASS_GENERIC_INDEX = 0;

    public static final int AGGREGATE_ROOT_CLASS_GENERIC_IDEX = 1;

    private Repositories() {}


    /**
     * Returns {@link Class} object representing the aggregate id type of the given repository.
     *
     * @return the aggregate id {@link Class}
     */
    public static <I extends Message> Class<I> getAggregateIdClass(Repository repository) {
        return ServerMethods.getGenericParameterType(repository, AGGREGATE_ID_CLASS_GENERIC_INDEX);
    }

    /**
     * Returns {@link Class} object representing the aggregate root type of the given repository.
     *
     * @return the aggregate root {@link Class}
     */
    public static <R extends AggregateRoot> Class<R> getAggregateRootClass(Repository repository) {
        return ServerMethods.getGenericParameterType(repository, AGGREGATE_ROOT_CLASS_GENERIC_IDEX);
    }
}
