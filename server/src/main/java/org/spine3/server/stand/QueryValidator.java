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

import com.google.common.base.Optional;
import org.spine3.base.Error;
import org.spine3.client.Query;
import org.spine3.client.QueryValidationError;

import static org.spine3.client.QueryValidationError.INVALID_QUERY;

/**
 * Validates the {@linkplain Query} instances submitted to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
class QueryValidator extends RequestValidator<Query> {
    @Override
    protected QueryValidationError getInvalidMessageErrorCode() {
        return INVALID_QUERY;
    }

    @Override
    protected InvalidQueryException onInvalidMessage(String exceptionMsg, Query request,
                                                     Error error) {
        return new InvalidQueryException(exceptionMsg, request, error);
    }

    @Override
    protected Optional<RequestNotSupported<Query>> isSupported(Query request) {
        return Optional.absent();
    }
}
