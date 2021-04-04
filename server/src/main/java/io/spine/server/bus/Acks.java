/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.bus;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.server.type.RejectionEnvelope;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * A utility for producing {@link Ack} instances.
 *
 * @deprecated please use {@link MessageExtensionsKt}.
 */
@Internal
@Deprecated
public final class Acks {

    /** Prevents instantiation of this utility class. */
    private Acks() {
    }

    /**
     * Creates an {@code Ack} with the {@code OK} status.
     *
     * @deprecated please use {@link MessageExtensionsKt}
     */
    @Deprecated
    public static Ack acknowledge(Message id) {
        checkNotNull(id);
        return MessageExtensionsKt.acknowledge(id);
    }

    /**
     * Creates an {@code Ack} with an error status.
     *
     * @param id
     *         the ID of the message which has been posted
     * @param cause
     *         the error
     * @deprecated please use {@link MessageExtensionsKt#causedError(Message, Error)}
     */
    @Deprecated
    public static Ack reject(Message id, Error cause) {
        checkNotDefaultArg(id);
        checkNotDefaultArg(cause);
        return MessageExtensionsKt.causedError(id, cause);
    }

    /**
     * Creates an {@code Ack} with the business rejection status.
     *
     * @deprecated please use {@link MessageExtensionsKt#reject(CommandId, Event)}
     */
    @Deprecated
    public static Ack reject(Message id, RejectionEnvelope cause) {
        checkNotNull(id);
        checkNotNull(cause);
        CommandId casted = (CommandId) id;
        Event rejection = cause.outerObject();
        return MessageExtensionsKt.reject(casted, rejection);
    }
}
