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

package org.spine3.net;

import org.junit.Test;
import org.spine3.net.Url.Record.QueryParameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection"})
public class QueryParametersShould {

    @Test
    public void fail_on_wrong_queries() {
        try {
            QueryParameters.parse("123");
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        try {
            QueryParameters.of("", "123");
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        try {
            QueryParameters.of("123", "");
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void convert_proper_parameters() {
        final String key = "keyOne";
        final String value = "valueTwo";

        final String query = key + '=' + value;

        final QueryParameter parameter1 = QueryParameters.parse(query);
        final QueryParameter parameter2 = QueryParameters.of(key, value);

        assertEquals(key, parameter1.getKey());
        assertEquals(value, parameter1.getValue());

        assertEquals(query, QueryParameters.toString(parameter1));
        assertEquals(query, QueryParameters.toString(parameter2));
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(QueryParameters.class);
    }
}
