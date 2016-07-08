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
package org.spine3.server.event.error;

import com.google.protobuf.Message;
import org.spine3.base.Error;
import org.spine3.base.EventValidationError;
import org.spine3.server.type.EventClass;
import org.spine3.protobuf.TypeUrl;

/**
 * Exception that is thrown when unsupported event is obtained
 * or in case there is no class for given Protobuf event message.
 *
 * @author Alexander Litus
 */
public class UnsupportedEventException extends EventException {

    public UnsupportedEventException(Message eventMsg) {
        super(messageFormat(eventMsg), eventMsg, unsupportedEventError(eventMsg));
    }

    private static String messageFormat(Message eventMsg) {
        final EventClass eventClass = EventClass.of(eventMsg);
        final TypeUrl typeUrl = TypeUrl.of(eventMsg);
        final String result = String.format(
                "There is no registered handler or dispatcher for the event of class: `%s`. Protobuf type: `%s`",
                eventClass, typeUrl
        );
        return result;
    }

    /**
     * Creates an instance of unsupported event error.
     */
    private static Error unsupportedEventError(Message eventMessage) {
        final String type = eventMessage.getDescriptorForType().getFullName();
        final String errMsg = String.format("Events of the type `%s` are not supported.", type);
        final Error.Builder error = Error.newBuilder()
                .setType(EventValidationError.getDescriptor().getFullName())
                .setCode(EventValidationError.UNSUPPORTED_EVENT.getNumber())
                .putAllAttributes(eventTypeAttribute(eventMessage))
                .setMessage(errMsg);
        return error.build();
    }

    private static final long serialVersionUID = 0L;
}
