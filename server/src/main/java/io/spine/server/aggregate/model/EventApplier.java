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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Empty;
import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAttribute;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Function;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
public final class EventApplier
        extends AbstractHandlerMethod<Aggregate,
                                      EventMessage,
                                      EventClass,
                                      EventEnvelope,
                                      Empty,
                                      MethodResult<Empty>> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *  @param method   the applier method
     * @param signature {@link ParameterSpec} which describes the method
     */
    EventApplier(Method method, ParameterSpec<EventEnvelope> signature) {
        super(method, signature);
    }

    /**
     * Adds {@link AllowImportAttribute} to the set of method attributes.
     */
    @Override
    protected Set<Function<Method, MethodAttribute<?>>> attributeSuppliers() {
        return Sets.union(super.attributeSuppliers(), ImmutableSet.of(AllowImportAttribute::of));
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    @Override
    protected MethodResult<Empty> toResult(Aggregate target, Object rawMethodOutput) {
        return MethodResult.empty();
    }

    boolean allowsImport() {
        return getAttributes().contains(AllowImportAttribute.ALLOW);
    }
}
