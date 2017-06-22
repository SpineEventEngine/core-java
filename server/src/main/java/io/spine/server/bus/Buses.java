/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.base.Error;
import io.spine.base.Failure;
import io.spine.base.IsSent;
import io.spine.base.Responses;
import io.spine.base.Status;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.validate.Validate.isNotDefault;

/**
 * A utility for working with {@link Bus buses} and related data types.
 *
 * @author Dmytro Dashenkov
 */
public class Buses {

    private Buses() {
        // Prevent utility class instantiation.
    }

    /**
     * Acknowledges the envelope posted.
     *
     * @param id the ID of the message to acknowledge
     * @return {@code IsSent} with an {@code OK} status
     */
    public static IsSent acknowledge(Message id) {
        return setStatus(id, Responses.statusOk());
    }

    /**
     * Creates {@code IsSent} response for the given message ID with the error status.
     *
     * @param id    the ID of the message to provide with the status
     * @param cause the cause of the message rejection
     * @return the {@code IsSent} response with the given message ID
     */
    public static IsSent reject(Message id, Error cause) {
        checkNotNull(cause);
        checkArgument(isNotDefault(cause));
        final Status status = Status.newBuilder()
                                    .setError(cause)
                                    .build();
        return setStatus(id, status);
    }

    /**
     * Creates {@code IsSent} response for the given message ID with the failure status.
     *
     * @param id    the ID of the message to provide with the status
     * @param cause the cause of the message rejection
     * @return the {@code IsSent} response with the given message ID
     */
    public static IsSent reject(Message id, Failure cause) {
        checkNotNull(cause);
        checkArgument(isNotDefault(cause));
        final Status status = Status.newBuilder()
                                    .setFailure(cause)
                                    .build();
        return setStatus(id, status);
    }

    private static IsSent setStatus(Message id, Status status) {
        checkNotNull(id);

        final Any packedId = pack(id);
        final IsSent result = IsSent.newBuilder()
                                    .setMessageId(packedId)
                                    .setStatus(status)
                                    .build();
        return result;
    }
}
