/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.client;

import io.spine.base.Field;
import io.spine.core.Event;
import io.spine.core.EventContext;

import static java.lang.String.format;

/**
 * Utility class for obtaining the {@code context} field in the {@code Event} proto type.
 *
 * @implNote This class uses field numbers instead of string constants, which is safer than
 *         using string constants for referencing proto fields. In the (unlikely) event of renaming
 *         the referenced fields, this class would still provide correct references to fields and
 *         their new names.
 */
final class EventContextField {

    /** References the {@code context} field in {@code Event}. */
    private static final Field CONTEXT_FIELD =
        Field.withNumberIn(Event.CONTEXT_FIELD_NUMBER, Event.getDescriptor());

    /** The name of the {@code context} field. */
    private static final String CONTEXT_FIELD_NAME = CONTEXT_FIELD.toString();

    private static final String PAST_MESSAGE = pastMessageField();

    /** Prevents instantiation of this utility class. */
    private EventContextField() {
    }

    /** Obtains the instance of the {@code context} field. */
    static Field instance() {
        return CONTEXT_FIELD;
    }

    /** Obtains the name of the {@code context} field. */
    static String name() {
        return CONTEXT_FIELD_NAME;
    }

    /**
     * Obtains the path to the "context.past_message" field of the {@code Event} proto type.
     */
     static String pastMessage() {
        return PAST_MESSAGE;
    }

    /**
     * Obtains the path to the "context.past_message" field of {@code Event} proto type.
     *
     * <p>This method is safer than using a string constant because it relies on field numbers,
     * rather than names (that might be changed).
     */
    private static String pastMessageField() {
        Field pastMessage = Field.withNumberIn(EventContext.PAST_MESSAGE_FIELD_NUMBER,
                                               EventContext.getDescriptor());
        return format("%s.%s", name(), pastMessage.toString());
    }
}
