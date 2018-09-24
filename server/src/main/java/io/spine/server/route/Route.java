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

package io.spine.server.route;

import com.google.protobuf.Message;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * Obtains one or more entity identifiers based on a message and its context.
 *
 * @param <M> the type of messages to get IDs from
 * @param <C> the type of message context
 * @param <R> the type of the route function result
 * @author Alexander Yevsyukov
 */
@FunctionalInterface
public interface Route<M extends Message, C extends Message, R>
        extends BiFunction<M, C, R>, Serializable {

    /**
     * Obtains entity ID(s) from the passed message and its context.
     *
     * @param message an event or a command message
     * @param context a context of the message
     * @return a set of entity identifiers
     * @apiNote
     * This method overrides the one from {@code BiFunction} for more clarity in Javadoc
     * references. Without overriding, it would be {@code #apply(Object, Object)},
     * which may be confusing in the context of event routing.
     */
    @SuppressWarnings("AbstractMethodOverridesAbstractMethod") // see @apiNote
    @Override
    R apply(M message, C context);
}
