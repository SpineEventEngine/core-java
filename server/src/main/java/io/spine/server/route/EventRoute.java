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

package io.spine.server.route;

import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.core.EventContext;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Obtains a set of entity IDs for which to deliver an event.
 *
 * @param <I> the type of entity IDs
 * @param <M> the type of event messages to get IDs from
 */
@FunctionalInterface
public interface EventRoute<I, M extends EventMessage> extends Multicast<I, M, EventContext> {

    /**
     * Creates an event route that obtains event producer ID from an {@code EventContext} and
     * returns it as a sole element of the immutable set.
     *
     * @param <I> the type of the entity IDs to which the event would be routed
     * @return new route instance
     */
    static <I> EventRoute<I, EventMessage> byProducerId() {
        return new ByContext<>();
    }

    /**
     * Creates an event route that obtains event producer ID from an {@code EventContext} and
     * returns it as a sole element of the the immutable set.
     *
     * @param <I>
     *         the type of the IDs of entities to which the event would be routed
     * @param idClass
     *         the class of identifiers
     * @return new route instance
     */
    static <I> EventRoute<I, EventMessage> byFirstMessageField(Class<I> idClass) {
        return new ByFirstMessageField<>(idClass);
    }

    /**
     * Returns the empty immutable set.
     *
     * @apiNote This is a convenience method for ignoring a type of messages when building
     *          a routing schema in a repository.
     */
    static <I> Set<I> noTargets() {
        return ImmutableSet.of();
    }

    /**
     * Creates an immutable singleton set with the passed ID.
     *
     * @apiNote This is a convenience method for customizing routing schemas when the target is
     *          only one entity.
     */
    static <I> Set<I> withId(I id) {
        checkNotNull(id);
        return ImmutableSet.of(id);
    }
}
