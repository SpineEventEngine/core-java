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

import com.google.protobuf.Message;
import io.spine.server.entity.Entity;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding a class of an {@linkplain Entity#getState() entity state}.
 *
 * @author Alex Tymchenko
 */
public final class EntityStateClass extends MessageClass<Message> {

    private static final long serialVersionUID = 0L;

    private EntityStateClass(Class<? extends Message> value) {
        super(value);
    }

    /**
     * Obtains the class of the state of the given entity.
     */
    public static EntityStateClass of(Entity entity) {
        checkNotNull(entity);
        Message state = entity.getState();
        return of(state);
    }

    /**
     * Creates an instance of {@code EntityStateClass} from the class of the given message.
     */
    public static EntityStateClass of(Message entityState) {
        checkNotNull(entityState);
        return from(entityState.getClass());
    }

    /**
     * Creates an instance of {@code EntityStateClass} from the given class.
     */
    public static EntityStateClass from(Class<? extends Message> value) {
        checkNotNull(value);
        return new EntityStateClass(value);
    }
}
