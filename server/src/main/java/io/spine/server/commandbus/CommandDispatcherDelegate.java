/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.MessageEnvelope;

import java.util.Set;

/**
 * A common interface for objects which need to dispatch the
 * {@linkplain io.spine.base.Command commands}, but are unable to implement
 * the {@linkplain io.spine.server.commandbus.CommandDispatcher CommandDispatcher}.
 *
 * <p>A typical example of a {@code CommandDispatcherDelegate} usage is a routine, which
 * simultaneously dispatches different types of {@linkplain com.google.protobuf.Message messages}
 * in addition to {@code Command}s.
 *
 * <p>In this case such a class would have to implement several
 * {@linkplain io.spine.server.bus.MessageDispatcher MessageDispatcher} child interfaces
 * (such as {@linkplain io.spine.server.commandbus.CommandDispatcher CommandDispatcher} or
 * {@linkplain io.spine.server.event.EventDispatcher EventDispatcher}). However, it is impossible
 * to implement the same {@link io.spine.server.bus.MessageDispatcher#getMessageClasses()
 * getMessageClasses()} method several times with the different types of {@code MessageClass}es
 * returned.
 *
 * <p>The same interference takes place in attempt to implement
 * {@link io.spine.server.bus.MessageDispatcher#dispatch(MessageEnvelope)
 * MessageDispatcher#dispatch(MessageEnvelope)} method with the different types of
 * {@code MessageEnvelope}s dispatches simultaneously.
 *
 * <p>That's why unlike {@linkplain io.spine.server.bus.MessageDispatcher MessageDispatcher},
 * this interface defines its own contract for declaring the dispatched
 * {@linkplain CommandClass command classes}, which does not interfere with the
 * {@code MessageDispatcher} API.
 *
 * @author Alex Tymchenko
 * @see DelegatingCommandDispatcher
 */
@Internal
public interface CommandDispatcherDelegate {

    Set<CommandClass> getCommandClasses();

    void dispatchCommand(CommandEnvelope envelope);
}
