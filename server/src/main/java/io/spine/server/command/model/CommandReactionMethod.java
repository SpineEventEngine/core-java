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
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.server.command.model.CommandReactionMethod.Result;
import io.spine.server.event.EventReceiver;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodResult;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

/**
 * A method which generates one or more command messages in response to an event.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandReactionMethod
        extends AbstractHandlerMethod<EventReceiver, EventClass, EventContext, Result>
        implements CommandingMethod<EventReceiver, EventClass, EventContext, Result> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    protected CommandReactionMethod(Method method) {
        super(method);
    }

    @Override
    protected Result toResult(EventReceiver target, Object rawMethodOutput) {
        return null;
    }

    @Override
    public EventClass getMessageClass() {
        return null;
    }

    /**
     * The result of a method which reacts on
     */
    public static final class Result extends MethodResult<Message> {

        protected Result(@Nullable Object output) {
            super(output);
        }
    }

}
