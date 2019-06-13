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
import io.spine.server.entity.model.EntityClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Handles the string representation of the {@linkplain DefaultRepository default repositories}.
 *
 * <p>Unlike regular repositories, the default ones do not have an entity name encoded in their
 * class name. Thus, the repositories should provide some relevant information via their
 * {@code toString()} methods.
 */
@Internal
public final class DefaultRepositoryString {

    /**
     * Prevents the utility class instantiation.
     */
    private DefaultRepositoryString() {
    }

    /**
     * Obtains string representation of a default repository which manages entities of the given
     * class.
     *
     * @param entityClass
     *         the managed entity class
     * @return the string representation of the repository
     */

}
