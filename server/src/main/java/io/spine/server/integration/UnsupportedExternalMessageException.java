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
package io.spine.server.integration;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.protobuf.AnyPacker;
import io.spine.server.bus.MessageUnhandled;

import static io.spine.server.integration.ExternalMessageValidationError.UNSUPPORTED_EXTERNAL_MESSAGE;
import static java.lang.String.format;

/**
 * Exception that is thrown when unsupported external message is obtained
 * or in case there is no class for the given Protobuf event message.
 *
 * @author Alex Tymchenko
 */
public class UnsupportedExternalMessageException
        extends RuntimeException implements MessageUnhandled {

    private static final long serialVersionUID = 0L;

    private final GeneratedMessageV3 externalMessage;

    public UnsupportedExternalMessageException(Message externalMessage) {
        super();
        if (externalMessage instanceof GeneratedMessageV3) {
            this.externalMessage = (GeneratedMessageV3) externalMessage;
        } else {
            // This is strange. However, let's preserve the value by packing it.
            this.externalMessage = AnyPacker.pack(externalMessage);
        }
    }

    @Override
    public Error asError() {
        String msgType = externalMessage.getDescriptorForType()
                                              .getFullName();
        String errMsg = format("External messages of the type `%s` are not supported.",
                                     msgType);
        int errorCode = UNSUPPORTED_EXTERNAL_MESSAGE.getNumber();
        String errorType = ExternalMessageValidationError.getDescriptor()
                                                               .getFullName();
        Error error = Error.newBuilder()
                                 .setType(errorType)
                                 .setCode(errorCode)
                                 .setMessage(errMsg)
                                 .build();
        return error;
    }

    @Override
    public Throwable asThrowable() {
        return this;
    }
}
