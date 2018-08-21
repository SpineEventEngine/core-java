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

package io.spine.server.event.model;

import com.google.protobuf.Empty;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.event.EventSubscriber;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 * @see Subscribe
 */
public final class EventSubscriberMethod
        extends AbstractHandlerMethod<EventSubscriber,
                                      EventClass,
                                      EventContext,
                                      MethodResult<Empty>> {

    /** Creates a new instance. */
    EventSubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @CanIgnoreReturnValue // since event subscriber methods do not return values
    @Override
    public MethodResult<Empty> invoke(EventSubscriber target,
                                      Message message,
                                      EventContext context) {
        ensureExternalMatch(context.getExternal());
        return super.invoke(target, message, context);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    @Override
    protected MethodResult<Empty> toResult(Object target, @Nullable Object rawMethodOutput) {
    protected MethodResult<Empty> toResult(EventSubscriber target, Object rawMethodOutput) {
        return MethodResult.empty();
    }
}
