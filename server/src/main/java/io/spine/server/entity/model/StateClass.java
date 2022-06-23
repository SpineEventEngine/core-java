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
package io.spine.server.entity.model;

import io.spine.base.EntityState;
import io.spine.server.entity.Entity;
import io.spine.server.type.EventClass;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class of an {@linkplain Entity#state() entity state}.
 */
public final class StateClass extends MessageClass<EntityState> {

    private static final long serialVersionUID = 0L;
    private static final EventClass UPDATE_EVENT = EventClass.from(EntityStateChanged.class);

    private StateClass(Class<? extends EntityState> value) {
        super(value);
    }

    /**
     * Obtains the class of the state of the given entity.
     */
    public static StateClass of(Entity entity) {
        checkNotNull(entity);
        EntityState state = entity.state();
        return of(state);
    }

    /**
     * Creates an instance of {@code EntityStateClass} from the class of the given message.
     */
    public static StateClass of(EntityState state) {
        checkNotNull(state);
        return from(state.getClass());
    }

    /**
     * Creates an instance of {@code EntityStateClass} from the given class.
     */
    public static StateClass from(Class<? extends EntityState> value) {
        checkNotNull(value);
        return new StateClass(value);
    }

    /**
     * Obtains the built-in class of events emitted when an entity state is updated.
     */
    public static EventClass updateEvent() {
        return UPDATE_EVENT;
    }
}
