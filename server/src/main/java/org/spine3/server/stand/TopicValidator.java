/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.stand;

import org.spine3.base.Error;
import org.spine3.client.Topic;
import org.spine3.client.TopicValidationError;

import static org.spine3.client.TopicValidationError.INVALID_TOPIC;

/**
 * Validates the {@linkplain Topic} instances submitted to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
class TopicValidator extends RequestValidator<Topic, TopicValidationError, InvalidTopicException> {

    @Override
    protected String getErrorText() {
        return "Topic message does not satisfy the validation constraints";
    }

    @Override
    protected TopicValidationError getErrorCode() {
        return INVALID_TOPIC;
    }

    @Override
    protected InvalidTopicException createException(String exceptionMsg,
                                                    Topic topic,
                                                    Error error) {
        return new InvalidTopicException(exceptionMsg, topic, error);
    }
}
