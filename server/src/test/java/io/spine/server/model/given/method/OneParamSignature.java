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

package io.spine.server.model.given.method;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.EventEnvelope;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static io.spine.server.model.declare.AccessModifier.PACKAGE_PRIVATE;
import static io.spine.server.model.declare.AccessModifier.PRIVATE;
import static io.spine.server.model.declare.AccessModifier.PROTECTED;
import static io.spine.server.model.declare.AccessModifier.PUBLIC;

public class OneParamSignature extends MethodSignature<OneParamMethod, EventEnvelope> {

    public OneParamSignature() {
        super(Annotation.class);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> getParamSpecs() {
        return ImmutableSet.copyOf(OneParamSpec.values());
    }

    @Override
    protected ImmutableSet<AccessModifier> getAllowedModifiers() {
        return allModifiers();
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return ImmutableSet.of(void.class);
    }

    @Override
    public OneParamMethod doCreate(Method method, ParameterSpec<EventEnvelope> parameterSpec,
                                   ImmutableSet<Class<? extends Message>> ignored) {
        return new OneParamMethod(method, parameterSpec);
    }

    private static ImmutableSet<AccessModifier> allModifiers() {
        return ImmutableSet.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
    }
}
