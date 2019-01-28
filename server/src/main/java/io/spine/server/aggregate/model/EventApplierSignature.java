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

package io.spine.server.aggregate.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.EventMessage;
import io.spine.core.EventEnvelope;
import io.spine.server.aggregate.Apply;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.model.declare.ReturnType;

import java.lang.reflect.Method;

import static io.spine.server.model.declare.MethodParams.consistsOfSingle;

/**
 * The signature of the {@link EventApplier} method.
 */
class EventApplierSignature extends MethodSignature<EventApplier, EventEnvelope> {

    EventApplierSignature() {
        super(Apply.class);
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return ImmutableSet.of(void.class);
    }

    @Override
    public EventApplier doCreate(Method method, ParameterSpec<EventEnvelope> parameterSpec,
                                 ReturnType returnType) {
        return new EventApplier(method, parameterSpec, returnType);
    }

    @Override
    protected ImmutableSet<AccessModifier> getAllowedModifiers() {
        return ImmutableSet.of(AccessModifier.PACKAGE_PRIVATE);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> getParamSpecs() {
        return ImmutableSet.copyOf(EventApplierParams.values());
    }

    /**
     * Allowed combinations of parameters for {@link EventApplier} methods.
     */
    @VisibleForTesting
    @Immutable
    enum EventApplierParams implements ParameterSpec<EventEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfSingle(methodParams, EventMessage.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope envelope) {
                return new Object[]{envelope.getMessage()};
            }
        }
    }
}
