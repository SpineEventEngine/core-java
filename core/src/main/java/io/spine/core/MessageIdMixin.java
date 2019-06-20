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

package io.spine.core;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * A mixin interface for the {@link MessageId} type.
 */
@GeneratedMixin
interface MessageIdMixin extends MessageIdOrBuilder {

    /**
     * Obtains the ID of the message.
     */
    default Message id() {
        return unpack(getId());
    }

    /**
     * Checks if the associated message is an event.
     */
    default boolean isEvent() {
        Any id = getId();
        return id.is(EventId.class);
    }

    /**
     * Obtains the {@link EventId} of the associated message.
     *
     * <p>Throws an {@link IllegalStateException} if the associated message is not an event.
     */
    default EventId asEventId() {
        checkState(isEvent(), "%s is not an event ID.", getId().getTypeUrl());
        return unpack(getId(), EventId.class);
    }

    /**
     * Checks if the associated message is a command.
     */
    default boolean isCommand() {
        Any id = getId();
        return id.is(CommandId.class);
    }

    /**
     * Obtains the {@link CommandId} of the associated message.
     *
     * <p>Throws an {@link IllegalStateException} if the associated message is not a command.
     */
    default CommandId asCommandId() {
        checkState(isCommand(), "%s is not a command ID.", getId().getTypeUrl());
        return unpack(getId(), CommandId.class);
    }
}
