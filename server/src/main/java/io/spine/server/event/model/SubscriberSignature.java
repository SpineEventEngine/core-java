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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;

/**
 * A signature of {@link SubscriberMethod}.
 */
public class SubscriberSignature extends EventAcceptingSignature<SubscriberMethod> {

    public SubscriberSignature() {
        super(Subscribe.class);
    }

    @Override
    protected ImmutableSet<AccessModifier> getAllowedModifiers() {
        return ImmutableSet.of(AccessModifier.PUBLIC);
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return ImmutableSet.of(void.class);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> getParamSpecs() {
        ImmutableSet<? extends ParameterSpec<EventEnvelope>> result = ImmutableSet
                .<ParameterSpec<EventEnvelope>>builder()
                .addAll(super.getParamSpecs())
                .addAll(ImmutableList.copyOf(EntityStateSubscriberSpec.values()))
                .build();
        return result;
    }

    @Override
    public SubscriberMethod doCreate(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        return isEntitySubscriber(method)
               ? new EntitySubscriberMethod(method, parameterSpec)
               : new EventSubscriberMethod(method, parameterSpec);
    }

    private static boolean isEntitySubscriber(Method method) {
        Class<?> firstParam = method.getParameterTypes()[0];
        return !EventMessage.class.isAssignableFrom(firstParam);
    }
}
