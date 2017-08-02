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

package io.spine.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.base.ThrowableMessage;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with rejections.
 *
 * @author Alexander Yevsyukov
 */
public final class Rejections {

    @VisibleForTesting
    static final String REJECTION_ID_FORMAT = "%s-reject";

    private Rejections() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Converts this {@code ThrowableMessage} into {@link Rejection}.
     *
     * @param command the command which caused the rejection
     */
    public static Rejection toRejection(ThrowableMessage message, Command command) {
        checkNotNull(message);
        checkNotNull(command);

        final Message state = message.getMessageThrown();
        final Any packedState = pack(state);
        final RejectionContext context = createContext(message, command);
        final RejectionId id = generateId(command.getId());
        return Rejection.newBuilder()
                        .setId(id)
                        .setMessage(packedState)
                        .setContext(context)
                        .build();
    }

    private static RejectionContext createContext(ThrowableMessage message, Command command) {
        final String stacktrace = Throwables.getStackTraceAsString(message);
        return RejectionContext.newBuilder()
                               .setTimestamp(message.getTimestamp())
                               .setStacktrace(stacktrace)
                               .setCommand(command)
                               .build();
    }

    /**
     * Generates a {@code RejectionId} based upon a {@linkplain CommandId command ID} in a format:
     *
     * <pre>{@code <commandId>-reject}</pre>
     *
     * @param id the identifier of the {@linkplain Command command}, which processing caused the
     *           rejection
     **/
    public static RejectionId generateId(CommandId id) {
        final String idValue = String.format(REJECTION_ID_FORMAT, id.getUuid());
        return RejectionId.newBuilder()
                          .setValue(idValue)
                          .build();
    }

    /**
     * Extracts the message from the passed {@code Rejection} instance.
     *
     * @param rejection a rejection to extract a message from
     * @param <M>       a type of the rejection message
     * @return an unpacked message
     */
    public static <M extends Message> M getMessage(Rejection rejection) {
        checkNotNull(rejection);
        final M result = unpack(rejection.getMessage());
        return result;
    }

    /**
     * Creates a new {@code Rejection} instance.
     *
     * @param messageOrAny the rejection message or {@code Any} containing the message
     * @param command      the {@code Command}, which triggered the rejection.
     * @return created rejection instance
     */
    public static Rejection createRejection(Message messageOrAny, Command command) {
        checkNotNull(messageOrAny);
        checkNotNull(command);

        final Any packedMessage = pack(messageOrAny);
        final RejectionContext context = RejectionContext.newBuilder()
                                                         .setCommand(command)
                                                         .build();
        final Rejection result = Rejection.newBuilder()
                                          .setMessage(packedMessage)
                                          .setContext(context)
                                          .build();
        return result;
    }

    /**
     * Obtains rejection producer ID from the passed {@code RejectionContext} and casts it to the
     * {@code <I>} type.
     *
     * @param context the rejection context to to get the producer ID
     * @param <I>     the type of the producer ID
     * @return the producer ID
     */
    public static <I> I getProducer(RejectionContext context) {
        checkNotNull(context);

        final I id = Identifier.unpack(context.getProducerId());
        return id;
    }
}
