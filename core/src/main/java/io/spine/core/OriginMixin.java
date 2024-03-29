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

package io.spine.core;

import com.google.protobuf.Descriptors;
import io.spine.annotation.GeneratedMixin;
import io.spine.annotation.Internal;
import io.spine.validate.FieldAwareMessage;

import static io.spine.protobuf.Messages.isNotDefault;

/**
 * A mixin interface for the {@link Origin} message type.
 */
@GeneratedMixin
interface OriginMixin extends OriginOrBuilder, FieldAwareMessage {

    default MessageId messageId() {
        return getMessage();
    }

    /**
     * Obtains the root origin message ID.
     *
     * <p>The root message has no further origin, as it is produced by an actor.
     */
    default MessageId root() {
        var root = this;
        while (isNotDefault(root.getGrandOrigin())) {
            root = root.getGrandOrigin();
        }
        return root.messageId();
    }

    @Override
    @Internal
    default Object readValue(Descriptors.FieldDescriptor field) {
        switch (field.getIndex()) {
            case 0:
                return getMessage();
            case 1:
                return getGrandOrigin();
            case 2:
                return getActorContext();
            default:
                return getField(field);
        }
    }
}
