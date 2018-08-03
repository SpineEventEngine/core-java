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

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.server.command.Command;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.HandlerMethodPredicate;
import io.spine.server.model.MethodResult;
import io.spine.type.MessageClass;

import java.util.List;

/**
 * Base interface for methods that generate one or more command messages in response to
 * an incoming message.
 *
 * @param <T> the type of the target object
 * @param <M> the type of the message class
 * @param <C> the type of the message context
 *
 * @author Alexander Yevsyukov
 */
@Immutable
public
interface CommandingMethod<T, M extends MessageClass, C extends Message, R extends MethodResult>
        extends HandlerMethod<T, M, C, R> {

    /**
     * Abstract base for commanding method predicates.
     */
    abstract class AbstractPredicate<C extends Message> extends HandlerMethodPredicate<C> {

        AbstractPredicate(Class<C> contextClass) {
            super(Command.class, contextClass);
        }
    }

    /**
     * A command substitution method returns a one or more command messages.
     */
    final class Result extends MethodResult<Message> {

        private final boolean optional;

        Result(Object rawMethodOutput, boolean optional) {
            super(rawMethodOutput);
            this.optional = optional;
            List<Message> messages = toMessages(rawMethodOutput);
            checkMessages(messages);
            setMessages(messages);
        }

        private void checkMessages(List<Message> messages) {
            if (messages.isEmpty() && !optional) {
                throw new IllegalStateException(
                        "Commanding method did not produce command messages"
                );
            }
        }
    }
}
