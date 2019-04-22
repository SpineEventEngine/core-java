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
package io.spine.server.commandbus;

import io.spine.annotation.Internal;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.util.Set;

/**
 * A common interface for objects which need to dispatch the
 * {@linkplain io.spine.core.Command commands}, but are unable to implement
 * the {@link io.spine.server.commandbus.CommandDispatcher CommandDispatcher}.
 *
 * <p>A typical example of a {@code CommandDispatcherDelegate} usage is a routine, which
 * simultaneously dispatches different types of {@linkplain com.google.protobuf.Message messages}
 * in addition to {@code Command}s.
 *
 * <p>In this case such a class would have to implement several
 * {@link io.spine.server.bus.MessageDispatcher MessageDispatcher} child interfaces
 * (such as {@link io.spine.server.commandbus.CommandDispatcher CommandDispatcher} or
 * {@link io.spine.server.event.EventDispatcher EventDispatcher}). However, it is impossible
 * to implement the same {@link io.spine.server.bus.MessageDispatcher#messageClasses()
 * getMessageClasses()} method several times with the different types of {@code MessageClass}es
 * returned.
 *
 * <p>The same interference takes place in attempt to implement
 * {@link io.spine.server.bus.UnicastDispatcher#dispatch(io.spine.server.type.MessageEnvelope)
 * UnicastDispatcher.dispatch(MessageEnvelope)} method with the different types of
 * {@code MessageEnvelope}s dispatches simultaneously.
 *
 * <p>That's why unlike {@linkplain io.spine.server.bus.MessageDispatcher MessageDispatcher},
 * this interface defines its own contract for declaring the dispatched
 * {@linkplain CommandClass command classes}, which does not interfere with the
 * {@code MessageDispatcher} API.
 *
 * @param <I> the type of IDs of entities that handle the commands dispatched by the delegate
 * @see DelegatingCommandDispatcher
 */
@Internal
public interface CommandDispatcherDelegate<I> {

    Set<CommandClass> commandClasses();

    I dispatchCommand(CommandEnvelope envelope);

    /**
     * Handles an error occurred during command dispatching.
     *
     * @param envelope  the event which caused the error
     * @param exception the error
     */
    void onError(CommandEnvelope envelope, RuntimeException exception);

    /**
     * Verifies if this instance dispatches at least one command.
     */
    default boolean dispatchesCommands() {
        return !commandClasses().isEmpty();
    }
}
