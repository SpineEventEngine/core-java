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

package io.spine.client;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.Message;

abstract class LoggingHandler {

    private final FluentLogger logger;
    private final String messageFormat;
    private final Class<? extends Message> type;

    /**
     * Creates new instance of the logging handler.
     *  @param logger
     *         the instance of the logger to use for reporting the error
     * @param messageFormat
     *         the formatting message where the first parameter is the consumer which caused
     * @param type
     *         the type of messages obtained by a consumer
     */
    LoggingHandler(FluentLogger logger, String messageFormat, Class<? extends Message> type) {
        this.logger = logger;
        this.messageFormat = messageFormat;
        this.type = type;
    }

    final FluentLogger logger() {
        return logger;
    }

    final String messageFormat() {
        return messageFormat;
    }

    final Class<? extends Message> type() {
        return type;
    }
}
