/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.Message;

/**
 * Logs the fact of an error caused by handling a message of the passed type.
 */
final class LoggingTypeErrorHandler extends LoggingHandlerWithType implements ErrorHandler {

    /**
     * Creates a new instance of the logging handler.
     *
     * @param logger
     *         the instance of the logger to use for reporting the error
     * @param messageFormat
     *         the formatting message with one string parameter is the name of the message type
     *         which caused the error
     * @param type
     *         the type of the message which caused the error
     */
    LoggingTypeErrorHandler(FluentLogger logger,
                            String messageFormat,
                            Class<? extends Message> type) {
        super(logger, messageFormat, type);
    }

    @Override
    public void accept(Throwable throwable) {
        error(throwable).log(messageFormat(), typeName());
    }
}
