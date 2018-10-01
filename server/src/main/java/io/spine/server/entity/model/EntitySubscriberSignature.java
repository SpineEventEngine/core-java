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

import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.declare.AccessModifier.PUBLIC;

/**
 * @author Dmytro Dashenkov
 */
public class EntitySubscriberSignature
        extends MethodSignature<EntitySubscriberMethod, EventEnvelope> {

    protected EntitySubscriberSignature() {
        super(Subscribe.class);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> getParamSpecs() {
        return copyOf(EntityStateSubscriberSpec.values());
    }

    @Override
    protected ImmutableSet<AccessModifier> getAllowedModifiers() {
        return of(PUBLIC);
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return of(void.class);
    }

    @Override
    public EntitySubscriberMethod doCreate(Method method,
                                           ParameterSpec<EventEnvelope> parameterSpec) {
        return new EntitySubscriberMethod(method, parameterSpec);
    }

    @Override
    protected boolean skipMethod(Method method) {
        boolean shouldSkip = super.skipMethod(method);
        if (shouldSkip) {
            return true;
        }
        Class<?> firstParameter = method.getParameterTypes()[0];
        boolean eventSubscriber = EventMessage.class.isAssignableFrom(firstParameter);
        return eventSubscriber;
    }
}
