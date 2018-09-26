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
package io.spine.server.event;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.core.EventValidationError;
import io.spine.server.bus.MessageUnhandled;
import io.spine.type.TypeName;

import static java.lang.String.format;

/**
 * Exception that is thrown when unsupported event is obtained
 * or in case there is no class for given Protobuf event message.
 *
 * @author Alexander Litus
 */
public class UnsupportedEventException extends EventException implements MessageUnhandled {

    private static final long serialVersionUID = 0L;

    public UnsupportedEventException(EventMessage eventMsg) {
        super(messageFormat(eventMsg), eventMsg, unsupportedEventError(eventMsg));
    }

    private static String messageFormat(Message eventMsg) {
        EventClass eventClass = EventClass.of(eventMsg);
        TypeName typeName = eventClass.getTypeName();
        String result = format(
                "There is no registered handler or dispatcher for the event of the class: `%s` " +
                " (proto type: `%s`).",
                eventClass,
                typeName);
        return result;
    }

    /** Creates an instance of unsupported event error. */
    private static Error unsupportedEventError(Message eventMessage) {
        String type = eventMessage.getDescriptorForType()
                                  .getFullName();
        String errMsg = format("Events of the type `%s` are not supported.", type);
        Error error = Error
                .newBuilder()
                .setType(EventValidationError.getDescriptor()
                                             .getFullName())
                .setCode(EventValidationError.UNSUPPORTED_EVENT.getNumber())
                .putAllAttributes(eventTypeAttribute(eventMessage))
                .setMessage(errMsg)
                .build();
        return error;
    }
}
