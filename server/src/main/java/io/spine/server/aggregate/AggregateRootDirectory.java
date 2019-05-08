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

package io.spine.server.aggregate;

import com.google.protobuf.Message;
import io.spine.annotation.SPI;

import java.util.Optional;

/**
 * A mapping of aggregate roots to the associated parts.
 *
 * <p>A complex aggregate is a number of simpler aggregate instances which represent the same domain
 * object from the different viewpoints. These aggregates are derived from the {@link AggregatePart}
 * class and are united by a single root.
 *
 * <p>In the directory, the aggregate root is represented by its type and the parts - by their
 * repositories.
 */
@SPI
public interface AggregateRootDirectory {

    /**
     * Associates the given aggregate part repository and the respective root type.
     */
    void register(AggregatePartRepository<?, ?, ?> repository);

    /**
     * Looks up an aggregate part repository by the type of the root and the type of the part state.
     *
     * <p>If a matching repository if registered, it is obtained by this method with no regard to
     * the visibility of the aggregate.
     *
     * @param rootClass
     *         the type of the aggregate root
     * @param partStateClass
     *         the type of the part state
     * @return the {@link AggregatePartRepository} or {@code Optional.empty()} if such a repository
     *         is not registered
     */
    Optional<? extends AggregatePartRepository<?, ?, ?>>
    findPart(Class<? extends AggregateRoot<?>> rootClass, Class<? extends Message> partStateClass);
}
