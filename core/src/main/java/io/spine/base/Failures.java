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

package io.spine.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with failures.
 *
 * @author Alexander Yevsyukov
 */
public final class Failures {

    @VisibleForTesting
    static final String FAILURE_ID_FORMAT = "%s-fail";

    private Failures() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Generates a {@code FailureId} based upon a {@linkplain CommandId command ID} in a format:
     *
     * <pre>{@code <commandId>-fail}</pre>
     *
     * @param id the identifier of the {@linkplain Command command}, which processing caused the
     *           failure
     **/
    public static FailureId generateId(CommandId id) {
        final String idValue = String.format(FAILURE_ID_FORMAT, id.getUuid());
        return FailureId.newBuilder()
                        .setValue(idValue)
                        .build();
    }

    /**
     * Extracts the message from the passed {@code Failure} instance.
     *
     * @param failure a failure to extract a message from
     * @param <M>     a type of the failure message
     * @return an unpacked message
     */
    public static <M extends Message> M getMessage(Failure failure) {
        checkNotNull(failure);
        final M result = unpack(failure.getMessage());
        return result;
    }

    /**
     * Creates a new {@code Failure} instance.
     *
     * @param messageOrAny the failure message or {@code Any} containing the message
     * @param command      the {@code Command}, which triggered the failure.
     * @return created failure instance
     */
    public static Failure createFailure(Message messageOrAny,
                                        Command command) {
        checkNotNull(messageOrAny);
        checkNotNull(command);

        final Any packedFailureMessage = pack(messageOrAny);
        final FailureContext context = FailureContext.newBuilder()
                                                     .setCommand(command)
                                                     .build();
        final Failure result = Failure.newBuilder()
                                      .setMessage(packedFailureMessage)
                                      .setContext(context)
                                      .build();
        return result;
    }
}
