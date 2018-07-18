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
package io.spine.server.stand;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.type.TypeName;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;
import io.spine.validate.ValidationError;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;

import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.validate.ConstraintViolations.toText;
import static java.lang.String.format;

/**
 * An abstract base of validators for the incoming requests to {@linkplain Stand}.
 *
 * @param <M> the type of request
 * @author Alex Tymchenko
 */
abstract class RequestValidator<M extends Message> {

    /**
     * Returns the error code to use in the {@linkplain Error validation error}, in case
     * the validated request message does not satisfy the validation constraints.
     */
    protected abstract ProtocolMessageEnum getInvalidMessageErrorCode();

    /**
     * Creates the exception to be thrown if the request {@code Message} is invalid.
     *
     * <p>Allows the descendants to create exceptions of custom types.
     */
    protected abstract InvalidRequestException onInvalidMessage(String exceptionMsg,
                                                                M request,
                                                                Error error);

    /**
     * Determines if this request is supported by the system.
     *
     * @param request the request to test
       @return a {@linkplain RequestNotSupported value object} holding
               the details of support absence,
               or {@code Optional.empty()} if the request is supported
     */
    protected abstract Optional<RequestNotSupported<M>> isSupported(M request);

    /**
     * Checks whether the passed {@code request} is valid:
     *
     * <ol>
     *      <li>as a {@code Message}, according to the constraints set in its Protobuf definition;
     *      <li>meaning it is supported by a target {@code Stand}
     *          and may be passed for the further processing.
     * </ol>
     *
     * <p>In case the validation is not successful, the provide {@code responseObserver}
     * is {@linkplain StreamObserver#onError(Throwable) notified of an error}.
     * Also, an {@code IllegalArgumentException} is thrown exposing the validation failure.
     *
     * @param request          the request {@code Message} to validate
     * @param responseObserver the observer to notify of a potenital validation error.
     * @throws IllegalArgumentException if the passed request is not valid
     */
    void validate(M request, StreamObserver<?> responseObserver) throws IllegalArgumentException {
        handleValidationResult(validateMessage(request).orElse(null), responseObserver);
        handleValidationResult(checkSupported(request).orElse(null), responseObserver);
    }

    /**
     * Handles the {@linkplain InvalidRequestException request validation exception},
     * if it is present.
     *
     * <p>The given {@code responseObserver} is fed with the exception details.
     * Also, the {@code exception} is thrown, wrapped as an {@code IllegalStateException}.
     */
    private static void handleValidationResult(@Nullable InvalidRequestException exception,
                                               StreamObserver<?> responseObserver) {
        if (exception != null) {
            feedToResponse(exception, responseObserver);
            throw newIllegalArgumentException(exception, exception.getMessage());
        }
    }

    /**
     * Checks whether this request is supported, forms the proper {@link Error error}
     * and packs it into an exception.
     *
     * @param request the request to check for support
     * @return an instance of exception or {@code Optional.empty()} if the request is supported.
     */
    private Optional<InvalidRequestException> checkSupported(M request) {
        Optional<RequestNotSupported<M>> supported = isSupported(request);
        if (!supported.isPresent()) {
            return Optional.empty();
        }

        RequestNotSupported<M> result = supported.get();

        ProtocolMessageEnum unsupportedErrorCode = result.getErrorCode();
        String errorMessage = result.getErrorMessage();
        String errorTypeName = unsupportedErrorCode.getDescriptorForType()
                                                         .getFullName();
        Error.Builder errorBuilder = Error.newBuilder()
                                                .setType(errorTypeName)
                                                .setCode(unsupportedErrorCode.getNumber())
                                                .setMessage(errorMessage);
        Error error = errorBuilder.build();

        InvalidRequestException exception = result.createException(errorMessage,
                                                                         request,
                                                                         error);
        return Optional.of(exception);
    }

    /**
     * Checks whether the {@code Message} of the given request conforms the constraints
     *
     * @param request the request message to validate.
     * @return an instance of exception,
     *         or {@code Optional.empty()} if the request message is valid.
     */
    private Optional<InvalidRequestException> validateMessage(M request) {
        List<ConstraintViolation> violations = MessageValidator.newInstance()
                                                                     .validate(request);
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        ValidationError validationError =
                ValidationError.newBuilder()
                               .addAllConstraintViolation(violations)
                               .build();
        ProtocolMessageEnum errorCode = getInvalidMessageErrorCode();
        String typeName = errorCode.getDescriptorForType()
                                         .getFullName();
        String errorTextTemplate = getErrorText(request);
        String errorText = format("%s %s",
                                        errorTextTemplate,
                                        toText(violations));

        Error.Builder errorBuilder = Error.newBuilder()
                                                .setType(typeName)
                                                .setCode(errorCode.getNumber())
                                                .setValidationError(validationError)
                                                .setMessage(errorText);
        Error error = errorBuilder.build();
        return Optional.of(onInvalidMessage(formatExceptionMessage(request), request, error));
    }

    private String formatExceptionMessage(M request) {
        return format("%s. See Error.getValidationError() for details.",
                      getErrorText(request));
    }

    protected String getErrorText(M request) {
        return format("%s message does not satisfy the validation constraints.",
                      TypeName.of(request)
                              .getSimpleName());
    }

    private static void feedToResponse(InvalidRequestException cause,
                                       StreamObserver<?> responseObserver) {
        StatusRuntimeException validationException = invalidArgumentWithCause(cause);
        responseObserver.onError(validationException);
    }

    /**
     * Value object holding the unsuccessful result of request validation upon its current support.
     *
     * @param <M> the type of request
     */
    protected abstract static class RequestNotSupported<M extends Message> {

        private final ProtocolMessageEnum errorCode;

        private final String errorMessage;

        /**
         * Creates an instance of {@code RequestNotSupported} value object
         * by the specific error code and the error message.
         */
        RequestNotSupported(ProtocolMessageEnum errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        /**
         * Creates an exception, signalizing that the request is not supported.
         *
         * <p>Allows the descendants to create exceptions of custom types.
         */
        protected abstract InvalidRequestException createException(String errorMessage,
                                                                   M request,
                                                                   Error error);

        private ProtocolMessageEnum getErrorCode() {
            return errorCode;
        }

        private String getErrorMessage() {
            return errorMessage;
        }
    }
}
