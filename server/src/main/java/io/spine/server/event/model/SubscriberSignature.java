/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ObjectArrays;
import io.spine.base.EntityState;
import io.spine.core.Subscribe;
import io.spine.server.model.AllowedParams;
import io.spine.server.model.ParameterSpec;
import io.spine.server.model.ReturnTypes;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;

import static io.spine.server.model.ReturnTypes.onlyVoid;

/**
 * A signature of {@link SubscriberMethod}.
 */
public class SubscriberSignature extends EventAcceptingSignature<SubscriberMethod> {

    private static final AllowedParams<EventEnvelope> PARAMS = new AllowedParams<>(combinedSpecs());

    private static ParameterSpec<EventEnvelope>[] combinedSpecs() {
        ParameterSpec<EventEnvelope>[] eventParams = EventAcceptingMethodParams.values();
        ParameterSpec<EventEnvelope>[] stateParams = StateSubscriberSpec.values();
        @SuppressWarnings("unchecked") // ensured by the content of the merged arrays.
        ParameterSpec<EventEnvelope>[] result =
                ObjectArrays.concat(eventParams, stateParams, ParameterSpec.class);
        return result;
    }

    public SubscriberSignature() {
        super(Subscribe.class);
    }

    @Override
    protected ReturnTypes returnTypes() {
        return onlyVoid();
    }

    @Override
    public AllowedParams<EventEnvelope> params() {
        return PARAMS;
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
