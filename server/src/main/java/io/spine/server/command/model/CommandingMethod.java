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

package io.spine.server.command.model;

import com.google.protobuf.Message;
import io.spine.server.command.Command;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.HandlerMethodPredicate;
import io.spine.type.MessageClass;

/**
 * Base interface for methods that generate one or more command messages in response to
 * an incoming message.
 *
 * @param <M> the type of the message class
 * @param <C> the type of the message context
 *
 * @author Alexander Yevsyukov
 */
public interface CommandingMethod<M extends MessageClass, C extends Message>
        extends HandlerMethod<M, C> {

    /**
     * Abstract base for commanding method predicates.
     */
    abstract class AbstractPredicate<C extends Message> extends HandlerMethodPredicate<C> {

        AbstractPredicate(Class<C> contextClass) {
            super(Command.class, contextClass);
        }
    }
}
