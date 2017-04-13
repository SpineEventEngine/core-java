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
package org.spine3.server.stand;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import org.spine3.base.Error;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.MessageValidator;
import org.spine3.validate.ValidationError;

import java.util.List;

import static java.lang.String.format;
import static org.spine3.validate.ConstraintViolations.toText;

/**
 * An abstract base of validators for the incoming requests to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
abstract class RequestValidator<R extends Message,
        C extends ProtocolMessageEnum,
        E extends InvalidRequestException> {

    void validate(R request) throws E {
        validateMessage(request);

    }

    private void validateMessage(R request) {
        final List<ConstraintViolation> violations = MessageValidator.newInstance()
                                                                     .validate(request);

        if (violations.isEmpty()) {
            return;
        }

        final ValidationError validationError =
                ValidationError.newBuilder()
                               .addAllConstraintViolation(violations)
                               .build();
        final C errorCode = getErrorCode();
        final String typeName = errorCode.getDescriptorForType()
                                         .getFullName();
        final String errorTextTemplate = getErrorText();
        final String errorText = format("%s %s",
                                        errorTextTemplate,
                                        toText(violations));

        final Error.Builder errorBuilder = Error.newBuilder()
                                                .setType(typeName)
                                                .setCode(errorCode.getNumber())
                                                .setValidationError(validationError)
                                                .setMessage(errorText);
        final Error error = errorBuilder.build();
        throw createException(formatExceptionMessage(), request, error);
    }

    private String formatExceptionMessage() {
        return format("%s. See Error.getValidationError() for details.",
                      getErrorText());
    }

    protected abstract String getErrorText();

    protected abstract C getErrorCode();

    protected abstract E createException(String exceptionMsg, R request, Error error);
}
