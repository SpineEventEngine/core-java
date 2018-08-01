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

package io.spine.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static java.lang.String.format;

/**
 * Utility class for working with rejections.
 *
 * @author Alexander Yevsyukov
 */
public final class Rejections {

    /** The name suffix for an outer class of generated rejection classes. */
    public static final String OUTER_CLASS_SUFFIX = "Rejections";

    /** The format string for ID of a {@link Rejection}. */
    @VisibleForTesting
    static final String REJECTION_ID_FORMAT = "%s-reject";

    /** Prevents instantiation of this utility class. */
    private Rejections() {
    }

    /**
     * Tells whether the passed message class represents a rejection message.
     */
    public static boolean isRejection(Class<? extends Message> messageClass) {
        checkNotNull(messageClass);
        Class<?> enclosingClass = messageClass.getEnclosingClass();
        if (enclosingClass == null) {
            return false; // Rejection messages are generated as inner static classes.
        }
        boolean hasCorrectSuffix = enclosingClass.getName()
                                                 .endsWith(OUTER_CLASS_SUFFIX);
        return hasCorrectSuffix;
    }

    /**
     * Converts this {@code ThrowableMessage} into {@link Rejection}.
     *
     * @param command the command which caused the rejection
     */
    public static Rejection toRejection(ThrowableMessage throwable, Command command) {
        throw new UnsupportedOperationException("Method toRejection is not implemented!");
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

        Any packedMessage = pack(messageOrAny);
        RejectionContext context = RejectionContext
                .newBuilder()
                .setCommand(command)
                .build();
        Rejection result = Rejection
                .newBuilder()
                .setMessage(packedMessage)
                .setContext(context)
                .build();
        return result;
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
        String idValue = format(REJECTION_ID_FORMAT, id.getUuid());
        return RejectionId.newBuilder()
                          .setValue(idValue)
                          .build();
    }

    /**
     * Extracts the message from the passed {@code Rejection} instance.
     *
     * @param rejection a rejection to extract a message from
     * @return an unpacked message
     */
    public static Message getMessage(Rejection rejection) {
        checkNotNull(rejection);
        Message result = unpack(rejection.getMessage());
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
    public static <I> Optional<I> getProducer(RejectionContext context) {
        checkNotNull(context);
        Any producerId = context.getProducerId();
        if (Any.getDefaultInstance()
               .equals(producerId)) {
            return Optional.empty();
        }
        I id = Identifier.unpack(producerId);
        return Optional.of(id);
    }

    /**
     * Analyzes the rejection context and determines if the rejection has been produced outside
     * of the current bounded context.
     *
     * @param context the context of rejection
     * @return {@code true} if the rejection is external, {@code false} otherwise
     */
    @Internal
    public static boolean isExternal(RejectionContext context) {
        checkNotNull(context);
        return context.getExternal();
    }

    /**
     * Verifies if the exception was {@linkplain Throwables#getRootCause(Throwable) caused} by
     * a command rejection.
     *
     * @param exception the exception to analyze
     * @return {@code true} if the exception was created because of a command rejection thrown,
     * {@code false} otherwise
     */
    public static boolean causedByRejection(Throwable exception) {
        //TODO:2017-07-26:alexander.yevsyukov: Check against CommandRejection
        // instead of ThrowableMessage when code generation allows customizing a custom
        // rejection types instead of `ThrowableMessage`.
        // See: https://github.com/SpineEventEngine/base/issues/20
        Throwable rootCause = Throwables.getRootCause(exception);
        boolean result = rootCause instanceof ThrowableMessage;
        return result;
    }

    /**
     * Retrieves the {@linkplain Throwables#getRootCause root cause} of the given {@link Throwable}
     * as a {@link ThrowableMessage}.
     *
     * <p>Throws an {@link IllegalArgumentException} if the root cause is not
     * a {@code ThrowableMessage}.
     *
     * @param throwable the {@link Throwable} wrapping a {@link ThrowableMessage}
     * @return the wrapped {@link ThrowableMessage}
     * @throws IllegalArgumentException upon an invalid {@link Throwable}
     *                                  {@linkplain Throwables#getRootCause root cause}
     */
    static ThrowableMessage getCause(Throwable throwable) throws IllegalArgumentException {
        checkNotNull(throwable);
        checkArgument(causedByRejection(throwable));
        Throwable cause = Throwables.getRootCause(throwable);
        return (ThrowableMessage) cause;
    }
}
