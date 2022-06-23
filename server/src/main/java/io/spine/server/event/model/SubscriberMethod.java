/*
 * Copyright 2022, TeamDev. All rights reserved.
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
import io.spine.annotation.Internal;
import io.spine.base.EventMessage;
import io.spine.server.event.EventSubscriber;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.ArgumentFilter;
import io.spine.server.model.DispatchKey;
import io.spine.server.model.MethodParams;
import io.spine.server.model.ParameterSpec;
import io.spine.server.model.SelectiveHandler;
import io.spine.server.model.VoidMethod;
import io.spine.server.type.EmptyClass;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static com.google.common.base.Suppliers.memoize;

/**
 * A method annotated with the {@link io.spine.core.Subscribe @Subscribe} annotation.
 *
 * <p>Such a method may have side effects, but provides no visible output.
 *
 * @see io.spine.core.Subscribe
 */
@Immutable
public abstract class SubscriberMethod
        extends AbstractHandlerMethod<EventSubscriber,
                                      EventMessage,
                                      EventClass,
                                      EventEnvelope,
                                      EmptyClass>
        implements VoidMethod<EventSubscriber, EventClass, EventEnvelope>,
                   SelectiveHandler<EventSubscriber, EventClass, EventEnvelope, EmptyClass> {

    @SuppressWarnings("Immutable") // because this `Supplier` is effectively immutable.
    private final Supplier<ArgumentFilter> filter = memoize(this::createFilter);

    protected SubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    /**
     * Applies {@link #filter() filter} if a specific one is supplied. Otherwise returns the
     * supplied {@code key}.
     */
    @Internal
    DispatchKey applyFilter(DispatchKey key) {
        ArgumentFilter filter = filter();
        return filter.acceptsAll()
               ? key
               : key.withFilter(filter);
    }

    @Override
    public MethodParams params() {
        return MethodParams.of(this);
    }

    @Override
    public EventClass messageClass() {
        return EventClass.from(rawMessageClass());
    }

    /**
     * Creates the filter for messages handled by this method.
     */
    protected abstract ArgumentFilter createFilter();

    @Override
    public final ArgumentFilter filter() {
        return filter.get();
    }

    /**
     * Checks if this method can handle the given event.
     *
     * <p>It is assumed that the type of the event is correct and only the field filter should be
     * checked.
     *
     * @param event
     *         the event to check
     * @return {@code true} if this method can handle the given event, {@code false} otherwise
     */
    final boolean canHandle(EventEnvelope event) {
        ArgumentFilter filter = filter();
        boolean result = filter.test(event.message());
        return result;
    }
}
