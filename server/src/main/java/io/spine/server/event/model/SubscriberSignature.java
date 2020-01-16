/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.reflect.TypeToken;
import io.spine.base.EntityState;
import io.spine.core.Subscribe;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;

/**
 * A signature of {@link SubscriberMethod}.
 */
public class SubscriberSignature extends EventAcceptingSignature<SubscriberMethod> {

    private static final ImmutableSet<ParameterSpec<EventEnvelope>>
            PARAM_SPEC = ImmutableSet
            .<ParameterSpec<EventEnvelope>>builder()
            .addAll(EventAcceptingSignature.PARAM_SPEC)
            .addAll(ImmutableList.copyOf(StateSubscriberSpec.values()))
            .build();

    private static final ImmutableSet<TypeToken<?>>
            RETURN_TYPE = ImmutableSet.of(TypeToken.of(void.class));

    public SubscriberSignature() {
        super(Subscribe.class);
    }

    @Override
    protected ImmutableSet<TypeToken<?>> returnTypes() {
        return RETURN_TYPE;
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> paramSpecs() {
        return PARAM_SPEC;
    }

    @Override
    public SubscriberMethod create(Method method, ParameterSpec<EventEnvelope> params) {
        return isEntitySubscriber(method)
               ? new StateSubscriberMethod(method, params)
               : new EventSubscriberMethod(method, params);
    }

    /**
     * This method never returns any results.
     */
    @Override
    public boolean mayReturnIgnored() {
        return true;
    }

    private static boolean isEntitySubscriber(Method method) {
        Class<?> firstParam = method.getParameterTypes()[0];
        return EntityState.class.isAssignableFrom(firstParam);
    }
}
