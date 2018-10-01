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

package io.spine.server.entity.model;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.EventEnvelope;
import io.spine.server.entity.EntityStateSubscriber;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;

/**
 * @author Dmytro Dashenkov
 */
public class EntitySubscriberMethod
        extends AbstractHandlerMethod<EntityStateSubscriber,
                                      Message,
                                      EntityStateClass,
                                      EventEnvelope,
                                      MethodResult<Empty>> {

    EntitySubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @Override
    protected MethodResult<Empty> toResult(EntityStateSubscriber target,
                                           Object rawMethodOutput) {
        return MethodResult.empty();
    }

    @Override
    public EntityStateClass getMessageClass() {
        return EntityStateClass.of(rawMessageClass());
    }
}
