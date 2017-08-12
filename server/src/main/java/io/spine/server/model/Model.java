/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.model;

import com.google.common.collect.Maps;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateClass;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Model {

    private static final Model INSTANCE = new Model();

    private final Map<Class<?>, HandlerClass<?>> handlerClasses = Maps.newConcurrentMap();

    public static Model getInstance() {
        return INSTANCE;
    }

    /** Prevent instantiation from outside. */
    private Model() {}

    public AggregateClass<?> asAggregateClass(Class<? extends Aggregate> cls) {
        checkNotNull(cls);
        HandlerClass<?> handlerClass = handlerClasses.get(cls);
        if (handlerClass == null) {
            handlerClass = AggregateClass.of(cls);
            handlerClasses.put(cls, handlerClass);
        }
        return (AggregateClass<?>)handlerClass;
    }
}
