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

package org.spine3.server;

import org.spine3.server.util.Classes;

import javax.annotation.CheckReturnValue;

/**
 * Utility class for obtaining repository type information.
 *
 * @author Alexander Yevsyukov
 */
public class RepositoryTypeInfo {
    /**
     * The index of the declaration of the generic type {@code I} in the {@link Repository} class.
     */
    private static final int ID_CLASS_GENERIC_INDEX = 0;

    /**
     * Returns {@link Class} of entity IDs of the passed repository.
     *
     * @return the aggregate id {@link Class}
     */
    @CheckReturnValue
    public static <I> Class<I> getIdClass(Class<? extends Repository> clazz) {
        return Classes.getGenericParameterType(clazz, ID_CLASS_GENERIC_INDEX);
    }

    private RepositoryTypeInfo() {
    }
}
