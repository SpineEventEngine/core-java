/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.annotation.Internal;
import io.spine.base.ThrowableMessage;

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
    private Rejections() {}

    /**
     * Tells whether the passed message class represents a rejection message.
     */
    public static boolean isRejection(Class<? extends Message> messageClass) {
        checkNotNull(messageClass);
        final Class<?> enclosingClass = messageClass.getEnclosingClass();
        if (enclosingClass == null) {
            return false; // Rejection messages are generated as inner static classes.
        }
        final boolean hasCorrectSuffix = enclosingClass.getName()
                                                       .endsWith(OUTER_CLASS_SUFFIX);
        return hasCorrectSuffix;
    }

    /**
     * Extracts a rejection message if the passed instance is {@link Rejection} or {@link Any},
     * otherwise returns the passed message.
     */
    public static Message ensureMessage(Message rejectionOrMessage) {
        checkNotNull(rejectionOrMessage);
        if (rejectionOrMessage instanceof Rejection) {
            return getMessage((Rejection) rejectionOrMessage);
        }
        return io.spine.protobuf.Messages.ensureMessage(rejectionOrMessage);
    }

    /**
     * Converts this {@code ThrowableMessage} into {@link Rejection}.
     *
     * @param command the command which caused the rejection
     */
    public static Rejection toRejection(ThrowableMessage throwable, Command command) {
        checkNotNull(throwable);
        checkNotNull(command);

        final Message rejectionMessage = throwable.getMessageThrown();
        final Any packedState = pack(rejectionMessage);
        final RejectionContext context = createContext(throwable, command);
        final RejectionId id = generateId(command.getId());
        final Rejection.Builder builder = Rejection.newBuilder()
                                                   .setId(id)
                                                   .setMessage(packedState)
                                                   .setContext(context);
        return builder.build();
    }

    private static RejectionContext createContext(ThrowableMessage message, Command command) {
        final String stacktrace = Throwables.getStackTraceAsString(message);
        final RejectionContext.Builder builder =
                RejectionContext.newBuilder()
                                .setTimestamp(message.getTimestamp())
                                .setStacktrace(stacktrace)
                                .setCommand(command);

        final Optional<Any> optional = message.producerId();
        if (optional.isPresent()) {
            builder.setProducerId(optional.get());
        }
        return builder.build();
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
     * Generates a {@code RejectionId} based upon a {@linkplain CommandId command ID} in a format:
     *
     * <pre>{@code <commandId>-reject}</pre>
     *
     * @param id the identifier of the {@linkplain Command command}, which processing caused the
     *           rejection
     **/
    public static RejectionId generateId(CommandId id) {
        final String idValue = format(REJECTION_ID_FORMAT, id.getUuid());
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
     * Obtains rejection producer ID from the passed {@code RejectionContext} and casts it to the
     * {@code <I>} type.
     *
     * @param context the rejection context to to get the producer ID
     * @param <I>     the type of the producer ID
     * @return the producer ID
     */
    public static <I> Optional<I> getProducer(RejectionContext context) {
        checkNotNull(context);
        final Any producerId = context.getProducerId();
        if (Any.getDefaultInstance().equals(producerId)) {
            return Optional.absent();
        }
        final I id = Identifier.unpack(producerId);
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
     *         {@code false} otherwise
     */
    public static boolean causedByRejection(Throwable exception) {
        //TODO:2017-07-26:alexander.yevsyukov: Check against CommandRejection
        // instead of ThrowableMessage when code generation allows customizing a custom
        // rejection types instead of `ThrowableMessage`.
        // See: https://github.com/SpineEventEngine/base/issues/20
        final Throwable rootCause = Throwables.getRootCause(exception);
        final boolean result = rootCause instanceof ThrowableMessage;
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
    static ThrowableMessage getCause(Throwable throwable)
            throws IllegalArgumentException {
        checkNotNull(throwable);
        checkArgument(causedByRejection(throwable));
        final Throwable cause = Throwables.getRootCause(throwable);
        return (ThrowableMessage) cause;
    }
}
