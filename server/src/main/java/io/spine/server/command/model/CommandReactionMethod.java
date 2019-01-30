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

package io.spine.server.command.model;

import io.spine.base.EventMessage;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.command.model.CommandingMethod.Result;
import io.spine.server.event.EventReceiver;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;

/**
 * A method which <em>may</em> generate one or more command messages in response to an event.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandReactionMethod
        extends AbstractHandlerMethod<EventReceiver,
                                      EventMessage,
                                      EventClass,
                                      EventEnvelope,
                                      CommandClass,
                                      Result>
        implements CommandingMethod<EventReceiver, EventClass, EventEnvelope> {

    CommandReactionMethod(Method method, ParameterSpec<EventEnvelope> signature) {
        super(method, signature);
    }

    @Override
    protected Result toResult(EventReceiver target, Object rawMethodOutput) {
        Result result = new Result(rawMethodOutput, true);
        return result;
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }
}
