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

package io.spine.server.bus;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Rejection;
import io.spine.core.Responses;
import io.spine.core.Status;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.validate.Validate.isNotDefault;

/**
 * A utility for working with {@link Bus buses} and related data types.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public class Buses {

    /** Prevents instantiation of this utility class. */
    private Buses() {}

    /**
     * Acknowledges the envelope posted.
     *
     * @param id the ID of the message to acknowledge
     * @return {@code Ack} with an {@code OK} status
     */
    public static Ack acknowledge(Message id) {
        return setStatus(id, Responses.statusOk());
    }

    /**
     * Creates {@code Ack} response for the given message ID with the error status.
     *
     * @param id    the ID of the message to provide with the status
     * @param cause the cause of the message rejection
     * @return the {@code Ack} response with the given message ID
     */
    public static Ack reject(Message id, Error cause) {
        checkNotNull(cause);
        checkArgument(isNotDefault(cause));
        Status status = Status.newBuilder()
                                    .setError(cause)
                                    .build();
        return setStatus(id, status);
    }

    /**
     * Creates {@code Ack} response for the given message ID with the rejection status.
     *
     * @param id    the ID of the message to provide with the status
     * @param cause the cause of the message rejection
     * @return the {@code Ack} response with the given message ID
     */
    public static Ack reject(Message id, Rejection cause) {
        checkNotNull(cause);
        checkArgument(isNotDefault(cause));
        Status status = Status.newBuilder()
                                    .setRejection(cause)
                                    .build();
        return setStatus(id, status);
    }

    private static Ack setStatus(Message id, Status status) {
        checkNotNull(id);

        Any packedId = pack(id);
        Ack result = Ack.newBuilder()
                              .setMessageId(packedId)
                              .setStatus(status)
                              .build();
        return result;
    }
}
