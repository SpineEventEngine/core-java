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

package org.spine3.server.event;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import org.spine3.base.Error;
import org.spine3.base.EventValidationError;
import org.spine3.base.Response;
import org.spine3.base.ValidationError;
import org.spine3.validate.options.ConstraintViolation;

import java.util.List;
import java.util.Map;

/**
 * Utility class for working with event validation.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class EventValidation {

    private EventValidation() {}

    /**
     * Attribute names for event-related business failures.
     */
    public interface Attribute {
        String EVENT_TYPE_NAME = "eventType";
    }

    /**
     * Creates a {@code Response} for getting an unsupported event, which is a programming error.
     */
    public static Response unsupportedEvent(Message event) {
        final String eventType = event.getDescriptorForType().getFullName();
        final String errMsg = String.format("Events of the type `%s` are not supported.", eventType);
        final Error error = Error.newBuilder()
                                 .setType(EventValidationError.getDescriptor().getFullName())
                                 .setCode(EventValidationError.UNSUPPORTED_EVENT.getNumber())
                                 .putAllAttributes(eventTypeAttribute(eventType))
                                 .setMessage(errMsg)
                                 .build();
        final Response response = Response.newBuilder()
                .setError(error)
                .build();
        return response;
    }

    /**
     * Creates a {@code Response} for getting an event with invalid fields (e.g., marked as "required" but not set).
     *
     * @param event an invalid event message
     * @param violations constraint violations found in event message
     */
    public static Response invalidEvent(Message event, List<ConstraintViolation> violations) {
        final String eventType = event.getDescriptorForType().getFullName();
        final ValidationError validationError = ValidationError.newBuilder()
                .addAllConstraintViolation(violations)
                .build();
        final Error error = Error.newBuilder()
                                 .setType(EventValidationError.getDescriptor().getFullName())
                                 .setCode(EventValidationError.INVALID_EVENT.getNumber())
                                 .setValidationError(validationError)
                                 .putAllAttributes(eventTypeAttribute(eventType))
                                 .setMessage("Event message is invalid.")
                                 .build();
        final Response response = Response.newBuilder()
                .setError(error)
                .build();
        return response;
    }

    private static Map<String, Value> eventTypeAttribute(String eventType) {
        return ImmutableMap.of(Attribute.EVENT_TYPE_NAME, Value.newBuilder().setStringValue(eventType).build());
    }
}
