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
import io.spine.annotation.GeneratedMixin;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.protobuf.AnyPacker.unpack;

@GeneratedMixin
public interface MessageQualifierMixin {

    Any getMessageId();

    default MessageId id() {
        return (MessageId) unpack(getMessageId());
    }

    default boolean isEvent() {
        Any id = getMessageId();
        return id.is(EventId.class);
    }

    default EventId asEventId() {
        checkState(isEvent(), "%s is not an event ID.", getMessageId().getTypeUrl());
        return unpack(getMessageId(), EventId.class);
    }

    default boolean isCommand() {
        Any id = getMessageId();
        return id.is(CommandId.class);
    }

    default CommandId asCommandId() {
        checkState(isCommand(), "%s is not a command ID.", getMessageId().getTypeUrl());
        return unpack(getMessageId(), CommandId.class);
    }
}
