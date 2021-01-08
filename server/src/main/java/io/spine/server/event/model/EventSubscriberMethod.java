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

import com.google.errorprone.annotations.Immutable;
import io.spine.server.event.EventSubscriber;
import io.spine.server.model.ArgumentFilter;
import io.spine.server.model.DispatchKey;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.EmptyClass;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;

/**
 * A wrapper for an event subscriber method.
 */
@Immutable
public final class EventSubscriberMethod extends SubscriberMethod
        implements RejectionHandler<EventSubscriber, EmptyClass> {

    /** Creates a new instance. */
    EventSubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @Override
    public EventAcceptingMethodParams parameterSpec() {
        return (EventAcceptingMethodParams) super.parameterSpec();
    }

    @Override
    public DispatchKey key() {
        DispatchKey dispatchKey = RejectionHandler.super.key();
        return applyFilter(dispatchKey);
    }

    @Override
    public ArgumentFilter createFilter() {
        ArgumentFilter result = ArgumentFilter.createFilter(rawMethod());
        return result;
    }

    @Override
    protected void checkAttributesMatch(EventEnvelope event) throws IllegalArgumentException {
        super.checkAttributesMatch(event);
        ensureExternalMatch(event.isExternal());
    }
}
