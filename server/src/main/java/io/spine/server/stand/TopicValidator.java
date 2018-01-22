/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
package io.spine.server.stand;

import com.google.common.base.Optional;
import io.spine.base.Error;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.client.TopicValidationError;
import io.spine.type.TypeUrl;

import static io.spine.client.TopicValidationError.INVALID_TOPIC;
import static io.spine.client.TopicValidationError.UNSUPPORTED_TOPIC_TARGET;
import static java.lang.String.format;

/**
 * Validates the {@linkplain Topic} instances submitted to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
class TopicValidator extends AbstractTargetValidator<Topic> {

    TopicValidator(TypeRegistry typeRegistry) {
        super(typeRegistry);
    }

    @Override
    protected TopicValidationError getInvalidMessageErrorCode() {
        return INVALID_TOPIC;
    }

    @Override
    protected InvalidTopicException onInvalidMessage(String exceptionMsg,
                                                     Topic topic,
                                                     Error error) {
        return new InvalidTopicException(exceptionMsg, topic, error);
    }

    @Override
    protected Optional<RequestNotSupported<Topic>> isSupported(Topic request) {
        final Target target = request.getTarget();
        final boolean targetSupported = checkTargetSupported(target);
        if (targetSupported) {
            return Optional.absent();
        }

        return Optional.of(missingInRegistry(getTypeOf(target)));
    }

    private static RequestNotSupported<Topic> missingInRegistry(TypeUrl topicTargetType) {
        final String errorMessage = format("The topic target type is not supported: %s",
                                           topicTargetType.getTypeName());
        return new RequestNotSupported<Topic>(UNSUPPORTED_TOPIC_TARGET, errorMessage) {

            @Override
            protected InvalidRequestException createException(String messageText,
                                                              Topic request,
                                                              Error error) {
                return new InvalidTopicException(messageText, request, error);
            }
        };
    }
}
