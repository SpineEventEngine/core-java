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
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Subscribe;
import io.spine.option.EntityOption;
import io.spine.option.OptionsProto;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.type.TypeName;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.ImmutableSet.of;
import static io.spine.option.EntityOption.Visibility.FULL;
import static io.spine.option.EntityOption.Visibility.SUBSCRIBE;
import static io.spine.option.EntityOption.Visibility.VISIBILITY_UNKNOWN;
import static io.spine.option.Options.option;
import static io.spine.server.model.declare.AccessModifier.PUBLIC;
import static io.spine.server.model.declare.MethodParams.consistsOfSingle;

/**
 * @author Dmytro Dashenkov
 */
public class EntitySubscriberSignature
        extends MethodSignature<EntitySubscriberMethod, EntityStateEnvelope<? ,?>> {

    protected EntitySubscriberSignature() {
        super(Subscribe.class);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EntityStateEnvelope<?, ?>>> getParamSpecs() {
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
                                           ParameterSpec<EntityStateEnvelope<?, ?>> parameterSpec) {
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

    @Immutable
    private enum EntityStateSubscriberSpec implements ParameterSpec<EntityStateEnvelope<?, ?>> {

        PARAM_SPEC;

        private static final Set<EntityOption.Visibility> allowedVisibilityModifiers =
                EnumSet.of(VISIBILITY_UNKNOWN, SUBSCRIBE, FULL);

        @Override
        public boolean matches(Class<?>[] methodParams) {
            boolean typeMatches = consistsOfSingle(methodParams, Message.class);
            if (!typeMatches) {
                return false;
            }
            @SuppressWarnings("unchecked") // Checked above.
                    Class<? extends Message> singleParam = (Class<? extends Message>) methodParams[0];
            TypeName messageType = TypeName.of(singleParam);
            Optional<EntityOption> entityOption = getEntityOption(messageType);
            if (!entityOption.isPresent()) {
                return false;
            }
            EntityOption entity = entityOption.get();
            if (visibleForSubscription(entity)) {
                return true;
            } else {
                throw new InsufficientVisibilityException(messageType, entity.getVisibility());
            }
        }

        @Override
        public Object[] extractArguments(EntityStateEnvelope<?, ?> envelope) {
            return new Object[]{envelope.getMessage()};
        }

        private static Optional<EntityOption> getEntityOption(TypeName messageType) {
            Descriptor descriptor = messageType.getMessageDescriptor();
            Optional<EntityOption> entityOption = option(descriptor, OptionsProto.entity);
            return entityOption;
        }

        private static boolean visibleForSubscription(EntityOption entity) {
            EntityOption.Visibility entityVisibility = entity.getVisibility();
            boolean result = allowedVisibilityModifiers.contains(entityVisibility);
            return result;
        }
    }
}
