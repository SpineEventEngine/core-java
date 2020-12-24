/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.core.EventContext;
import io.spine.server.entity.EntityVisibility;
import io.spine.server.model.MethodParams;
import io.spine.server.model.ParameterSpec;
import io.spine.server.model.TypeMatcher;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.type.TypeName;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.model.TypeMatcher.classImplementing;
import static io.spine.server.model.TypeMatcher.exactly;

/**
 * A {@link ParameterSpec} of an entity state subscriber method.
 */
@Immutable
enum StateSubscriberSpec implements ParameterSpec<EventEnvelope> {

    MESSAGE(classImplementing(EntityState.class)) {
        @Override
        protected Object[] arrangeArguments(EntityState<?> state, EventEnvelope event) {
            return new Object[]{state};
        }
    },

    MESSAGE_AND_EVENT_CONTEXT(classImplementing(EntityState.class), exactly(EventContext.class)) {
        @Override
        protected Object[] arrangeArguments(EntityState<?> state, EventEnvelope event) {
            return new Object[]{state, event.context()};
        }
    };

    private final ImmutableList<TypeMatcher> criteria;

    StateSubscriberSpec(TypeMatcher... criteria) {
        this.criteria = ImmutableList.copyOf(criteria);
    }

    @Override
    public boolean matches(MethodParams params) {
        if (!params.match(criteria)) {
            return false;
        }
        @SuppressWarnings("unchecked") // Checked above.
        Class<? extends EntityState<?>> firstParameter =
                (Class<? extends EntityState<?>>) params.type(0);
        Optional<EntityVisibility> visibilityOption = EntityVisibility.of(firstParameter);
        if (!visibilityOption.isPresent()) {
            return false;
        }
        EntityVisibility visibility = visibilityOption.get();
        if (visibility.canSubscribe()) {
            return true;
        } else {
            throw new InsufficientVisibilityError(TypeName.of(firstParameter), visibility);
        }
    }

    @Override
    public Object[] extractArguments(EventEnvelope event) {
        EventMessage eventMessage = event.message();
        checkArgument(eventMessage instanceof EntityStateChanged,
                      "Must be an `%s` event.", EntityStateChanged.class.getSimpleName());
        EntityStateChanged systemEvent = (EntityStateChanged) eventMessage;
        EntityState<?> state = (EntityState<?>) unpack(systemEvent.getNewState());
        return arrangeArguments(state, event);
    }

    protected abstract Object[] arrangeArguments(EntityState<?> state, EventEnvelope event);
}
