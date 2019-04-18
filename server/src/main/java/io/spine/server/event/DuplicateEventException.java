/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.event;

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.type.TypeName;

import static java.lang.String.format;

/**
 * Reports an attempt to dispatch a duplicate event.
 *
 * <p>An event is considered a duplicate when its ID matches the ID of another event which was
 * already dispatched to a given entity.
 */
public final class DuplicateEventException extends RuntimeException {

    private static final String MESSAGE =
            "The event %s (ID: %s) cannot be dispatched to a single entity twice.";

    private static final long serialVersionUID = 0L;

    public DuplicateEventException(Event event) {
        super(messageFrom(event));
    }

    private static String messageFrom(Event event) {
        EventId eventId = event.getId();
        Message eventMessage = event.enclosedMessage();
        TypeName eventType = TypeName.of(eventMessage);
        String result = format(MESSAGE, eventType, eventId.getValue());
        return result;
    }
}
