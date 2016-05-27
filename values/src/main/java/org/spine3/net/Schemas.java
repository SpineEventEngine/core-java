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

import com.google.common.collect.ImmutableMap;
import org.spine3.net.Url.Record.Schema;

import java.util.Map;

/**
 * Utility class for working with {@link Schema}.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
/* package */ class Schemas {

    private Schemas() {
    }

    /**
     * Parses schema from String representation.
     *
     * @param value String schema representation.
     * @return {@link Schema} value
     */
    /* package */ static Schema parse(String value) {
        final String lowercaseValue = value.toLowerCase();
        if (!stringSchemas.containsKey(lowercaseValue)) {
            return Schema.UNDEFINED;
        }
        return stringSchemas.get(lowercaseValue);
    }

    /**
     * Obtains the lowercase schema name.
     *
     * @param schema {@link Schema} value
     * @return String name
     */
    /* package */ static String getLowerCaseName(Schema schema) {
        return schema.name().toLowerCase();
    }

    private static final Map<String, Schema> stringSchemas = buildSchemasMap();

    private static Map<String, Schema> buildSchemasMap() {
        final ImmutableMap.Builder<String, Schema> schemas = new ImmutableMap.Builder<>();

        for (Schema schema : Schema.values()) {
            schemas.put(getLowerCaseName(schema), schema);
        }

        return schemas.build();
    }
}
