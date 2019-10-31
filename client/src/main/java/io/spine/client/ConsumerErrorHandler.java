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
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Message;

import java.util.function.BiConsumer;

import static io.spine.client.DelegatingConsumer.toRealConsumer;

/**
 * Functional interface for handlers of errors caused by consumers of messages.
 *
 * @param <M>
 *         the type of messages delivered to consumers
 */
@FunctionalInterface
public interface ConsumerErrorHandler<M extends Message>
        extends BiConsumer<MessageConsumer<M, ?>, Throwable> {

    /**
     * Logs the fact of the error using the {@linkplain FluentLogger#atSevere() server} level
     * of the passed logger.
     *
     * @param logger
     *         the instance of the logger to use for reporting the error
     * @param messageFormat
     *         the formatting message where the first parameter is the consumer which caused
     *         the error, and the second parameter is the type of the message which caused the error
     * @param <M>
     *         the type of the messages delivered to the consumer
     * @return the logging error handler
     */
    static <M extends Message> ConsumerErrorHandler<M>
    logError(FluentLogger logger, String messageFormat) {
        return (consumer, throwable) -> {
            Class<? super M> type = new TypeToken<M>(){}.getRawType();
            Object consumerToReport = toRealConsumer(consumer);
            logger.atSevere()
                  .withCause(throwable)
                  .log(messageFormat, consumerToReport, type);
        };
    }

}
