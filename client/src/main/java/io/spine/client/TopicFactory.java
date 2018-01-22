/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.ActorContext;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Targets.composeTarget;

/**
 * Public API for creating {@link Topic} instances, using the {@code ActorRequestFactory}
 * configuration.
 *
 * @see ActorRequestFactory#topic()
 */
public final class TopicFactory {

    private final ActorContext actorContext;

    TopicFactory(ActorRequestFactory actorRequestFactory) {
        checkNotNull(actorRequestFactory);
        this.actorContext = actorRequestFactory.actorContext();
    }

    /**
     * Creates a {@link Topic} for a subset of the entity states by specifying their IDs.
     *
     * @param entityClass the class of a target entity
     * @param ids         the IDs of interest
     * @return the instance of {@code Topic} assembled according to the parameters.
     */
    public Topic someOf(Class<? extends Message> entityClass, Set<? extends Message> ids) {
        checkNotNull(entityClass);
        checkNotNull(ids);

        final Target target = composeTarget(entityClass, ids, null);
        final Topic result = forTarget(target);
        return result;
    }

    /**
     * Creates a {@link Topic} for all of the specified entity states.
     *
     * @param entityClass the class of a target entity
     * @return the instance of {@code Topic} assembled according to the parameters.
     */
    public Topic allOf(Class<? extends Message> entityClass) {
        checkNotNull(entityClass);

        final Target target = composeTarget(entityClass, null, null);
        final Topic result = forTarget(target);
        return result;
    }

    /**
     * Creates a {@link Topic} for the specified {@linkplain Target}.
     *
     * <p>This method is intended for internal use only. To achieve the similar result,
     * {@linkplain #allOf(Class) allOf()} and {@linkplain #someOf(Class, Set) someOf()} methods
     * should be used.
     *
     * @param target the {@code} Target to create a topic for.
     * @return the instance of {@code Topic}.
     */
    @Internal
    public Topic forTarget(Target target) {
        checkNotNull(target);
        final TopicId id = Topics.generateId();
        return Topic.newBuilder()
                    .setId(id)
                    .setContext(actorContext)
                    .setTarget(target)
                    .build();
    }

}
