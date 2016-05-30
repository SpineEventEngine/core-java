/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.base;

import org.junit.Test;
import org.spine3.validate.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ResponsesShould {

    private static final Response RESPONSE_UNSUPPORTED_EVENT = Response.newBuilder()
            .setError(Error.newBuilder()
                           .setCode(EventValidationError.UNSUPPORTED_EVENT.getNumber()))
            .build();

    private static final Response RESPONSE_INVALID_MESSAGE = newInvalidMessageResponse();

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Responses.class));
    }

    @Test
    public void return_OK_response() {
        checkNotNull(Responses.ok());
    }

    @Test
    public void recognize_OK_response() {
        assertTrue(Responses.isOk(Responses.ok()));
    }

    @Test
    public void return_false_if_not_OK_response() {
        assertFalse(Responses.isOk(RESPONSE_INVALID_MESSAGE));
    }

    @Test
    public void recognize_UNSUPPORTED_EVENT_response() {
        assertTrue(Responses.isUnsupportedEvent(RESPONSE_UNSUPPORTED_EVENT));
    }

    @Test
    public void return_false_if_not_UNSUPPORTED_EVENT_response() {
        assertFalse(Responses.isUnsupportedEvent(Responses.ok()));
    }

    @Test
    public void recognize_INVALID_MESSAGE_response() {
        assertTrue(Responses.isInvalidMessage(RESPONSE_INVALID_MESSAGE));
    }

    @Test
    public void return_false_if_not_INVALID_MESSAGES_response() {
        assertFalse(Responses.isInvalidMessage(Responses.ok()));
    }

    private static Response newInvalidMessageResponse() {
        final List<ConstraintViolation> violations = newArrayList(ConstraintViolation.getDefaultInstance());
        final ValidationError validationError = ValidationError.newBuilder()
                                                               .addAllConstraintViolation(violations)
                                                               .build();
        final Error error = Error.newBuilder()
                                 .setValidationError(validationError)
                                 .build();
        final Response response = Response.newBuilder()
                                          .setError(error)
                                          .build();
        return response;
    }
}
