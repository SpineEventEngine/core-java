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
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.code.proto.EntityStateOption;
import io.spine.core.EventContext;
import io.spine.option.EntityOption;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.type.TypeName;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.immutableEnumSet;
import static io.spine.option.EntityOption.Visibility.FULL;
import static io.spine.option.EntityOption.Visibility.SUBSCRIBE;
import static io.spine.option.EntityOption.Visibility.VISIBILITY_UNKNOWN;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.model.declare.MethodParams.consistsOfTypes;

/**
 * A {@link ParameterSpec} of an entity state subscriber method.
 */
@Immutable
enum EntityStateSubscriberSpec implements ParameterSpec<EventEnvelope> {

    MESSAGE(ImmutableList.of(Message.class)) {
        @Override
        protected Object[] arrangeArguments(Message entityState, EventEnvelope event) {
            return new Object[]{entityState};
        }
    },

    MESSAGE_AND_EVENT_CONTEXT(ImmutableList.of(Message.class, EventContext.class)) {
        @Override
        protected Object[] arrangeArguments(Message entityState, EventEnvelope event) {
            return new Object[]{entityState, event.context()};
        }
    };

    /**
     * The set of {@link io.spine.option.EntityOption.Visibility Visibility} modifiers which allow
     * an entity state to be subscribed to.
     */
    private static final Set<EntityOption.Visibility> allowedVisibilityModifiers =
            immutableEnumSet(VISIBILITY_UNKNOWN, SUBSCRIBE, FULL);

    private final ImmutableList<Class<?>> parameters;

    EntityStateSubscriberSpec(ImmutableList<Class<?>> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean matches(Class<?>[] methodParams) {
        boolean typesMatch = consistsOfTypes(methodParams, parameters);
        if (!typesMatch) {
            return false;
        }
        @SuppressWarnings("unchecked") // Checked above.
        Class<? extends Message> firstParameter = (Class<? extends Message>) methodParams[0];
        TypeName messageType = TypeName.of(firstParameter);
        Optional<EntityOption> entityOption = getEntityOption(messageType);
        if (!entityOption.isPresent()) {
            return false;
        }
        EntityOption entity = entityOption.get();
        if (visibleForSubscription(entity)) {
            return true;
        } else {
            throw new InsufficientVisibilityError(messageType, entity.getVisibility());
        }
    }

    @Override
    public Object[] extractArguments(EventEnvelope event) {
        EventMessage eventMessage = event.message();
        checkArgument(eventMessage instanceof EntityStateChanged,
                      "Must be an %s event.", EntityStateChanged.class.getSimpleName());
        EntityStateChanged systemEvent = (EntityStateChanged) eventMessage;
        Message entityState = unpack(systemEvent.getNewState());
        return arrangeArguments(entityState, event);
    }

    protected abstract Object[] arrangeArguments(Message entityState, EventEnvelope event);

    private static Optional<EntityOption> getEntityOption(TypeName messageType) {
        Descriptor descriptor = messageType.messageDescriptor();
        Optional<EntityOption> result = EntityStateOption.valueOf(descriptor);
        return result;
    }

    private static boolean visibleForSubscription(EntityOption entity) {
        EntityOption.Visibility entityVisibility = entity.getVisibility();
        boolean result = allowedVisibilityModifiers.contains(entityVisibility);
        return result;
    }
}
