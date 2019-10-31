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
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.Message;

import java.util.function.Consumer;

/**
 * Functional interface for error handlers.
 */
@FunctionalInterface
public interface ErrorHandler extends Consumer<Throwable> {

    /**
     * Logs the fact of the error using the {@linkplain FluentLogger#atSevere() server} level
     * of the passed logger.
     *
     * @param logger
     *         the instance of the logger to use for reporting the error
     * @param messageFormat
     *         the formatting message where the sole parameter is the type of
     *         the message which caused the error
     * @param <M>
     *         the type of the messages delivered to the consumer
     * @return the logging error handler
     */
    static <M extends Message> ErrorHandler
    logError(FluentLogger logger, String messageFormat) {
        return (throwable) -> {
            Class<? super M> type = new TypeToken<M>(){}.getRawType();
            logger.atSevere()
                  .withCause(throwable)
                  .log(messageFormat, type);
        };
    }
}
