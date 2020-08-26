/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.Responses;
import io.spine.core.Status;
import io.spine.server.event.RejectionEnvelope;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * A utility for producing {@link Ack} instances.
 */
@Internal
public final class AckFactory {

    /** Prevents instantiation of this utility class. */
    private AckFactory() {
    }

    /**
     * Creates an {@code Ack} with the {@code OK} status.
     *
     * @param id
     *         the ID of the message which has been posted
     */
    public static Ack acknowledge(Message id) {
        checkNotNull(id);
        return setStatus(id, Responses.statusOk());
    }

    /**
     * Creates an {@code Ack} with an error status.
     *
     * @param id
     *         the ID of the message which has been posted
     * @param cause
     *         the error
     */
    public static Ack reject(Message id, Error cause) {
        checkNotDefaultArg(id);
        checkNotDefaultArg(cause);
        Status status = Status
                .newBuilder()
                .setError(cause)
                .build();
        return setStatus(id, status);
    }

    /**
     * Creates an {@code Ack} with the business rejection status.
     *
     * @param id
     *         the ID of the message which have been posted
     * @param cause
     *         the cause of the rejection
     */
    public static Ack reject(Message id, RejectionEnvelope cause) {
        checkNotNull(id);
        checkNotNull(cause);
        Event event = cause.outerObject();
        checkNotDefaultArg(event);
        Status status = Status
                .newBuilder()
                .setRejection(event)
                .build();
        return setStatus(id, status);
    }

    private static Ack setStatus(Message id, Status status) {
        Any packedId = pack(id);
        Ack result = Ack
                .newBuilder()
                .setMessageId(packedId)
                .setStatus(status)
                .build();
        return result;
    }
}
