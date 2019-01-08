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
package io.spine.server.stand;

import com.google.protobuf.ProtocolMessageEnum;
import io.spine.base.Error;
import io.spine.client.Query;
import io.spine.client.QueryValidationError;
import io.spine.type.TypeUrl;

import static io.spine.client.QueryValidationError.INVALID_QUERY;
import static io.spine.client.QueryValidationError.UNSUPPORTED_QUERY_TARGET;
import static java.lang.String.format;

/**
 * Validates the {@linkplain Query} instances submitted to {@linkplain Stand}.
 */
class QueryValidator extends AbstractTargetValidator<Query> {

    QueryValidator(TypeRegistry typeRegistry) {
        super(typeRegistry);
    }

    @Override
    protected QueryValidationError invalidMessageErrorCode() {
        return INVALID_QUERY;
    }

    @Override
    protected ProtocolMessageEnum unsupportedTargetErrorCode() {
        return UNSUPPORTED_QUERY_TARGET;
    }

    @Override
    protected InvalidQueryException invalidMessageException(String exceptionMsg,
                                                            Query request,
                                                            Error error) {
        return new InvalidQueryException(exceptionMsg, request, error);
    }

    @Override
    protected boolean isSupported(Query request) {
        return targetSupported(request.getTarget());
    }

    @Override
    protected InvalidRequestException unsupportedException(Query request, Error error) {
        String message = errorMessage(request);
        return new InvalidQueryException(message, request, error);
    }

    @Override
    protected String errorMessage(Query request) {
        TypeUrl targetType = getTypeOf(request.getTarget());
        return format("The query target type is not supported: %s", targetType.getTypeName());
    }
}
