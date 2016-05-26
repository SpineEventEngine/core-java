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

package org.spine3.net;

import org.spine3.net.Url.Record.QueryParameter;

/**
 * The utility class, which helps performing URL query parameters parsing and String conversion.
 */
@SuppressWarnings("UtilityClass")
public class QueryParameters {

    private static final char SEPARATOR = '=';

    private QueryParameters() {
    }

    /**
     * Performs parsing of {@link QueryParameter} from String.
     *
     * @param queryParameter String representation of {@link QueryParameter}
     * @return parsed instance
     * @throws IllegalArgumentException in case of not well formed argument value
     */
    public static QueryParameter of(String queryParameter) {
        final int separatorIndex = queryParameter.indexOf(SEPARATOR);

        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Query Parameter is invalid: " + queryParameter);
        }

        final String key = queryParameter.substring(0, separatorIndex);
        final String value = queryParameter.substring(separatorIndex + 1);

        final QueryParameter result = QueryParameter.newBuilder()
                                                    .setKey(key)
                                                    .setValue(value)
                                                    .build();
        return result;
    }

    /**
     * Performs String conversion of {@link QueryParameter}.
     *
     * @param param {@link QueryParameter} instance
     * @return key=value String
     */
    public static String toString(QueryParameter param) {
        final String result = param.getKey() + SEPARATOR + param.getValue();
        return result;
    }
}
