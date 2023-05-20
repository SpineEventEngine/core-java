/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.event.store;

import com.google.protobuf.Any;
import com.google.protobuf.util.Timestamps;
import io.spine.client.CompositeFilter;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;
import io.spine.client.TargetFilters;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.server.event.store.QueryToFilters.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QueryToFilters should")
class QueryToFiltersTest {

    @Test
    @DisplayName("convert empty query to empty EntityFilters")
    void convertEmptyToFilters() {
        EventStreamQuery query = EventStreamQuery.newBuilder()
                                                 .build();
        TargetFilters entityFilters = convert(query);
        assertTrue(entityFilters.getFilterList()
                                .isEmpty());
    }

    @Test
    @DisplayName("convert time query to EntityFilters")
    void convertTimeToFilters() {
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .setAfter(Timestamps.MIN_VALUE)
                .setBefore(Timestamps.MAX_VALUE)
                .build();
        TargetFilters entityFilters = convert(query);
        assertEquals(1, entityFilters.getFilterCount());

        CompositeFilter compositeFilter = entityFilters.getFilter(0);
        List<Filter> filters = compositeFilter.getFilterList();
        assertEquals(CompositeOperator.ALL, compositeFilter.getOperator());
        assertEquals(2, filters.size());
    }

    @Test
    @DisplayName("convert type query to EntityFilters")
    void convertTypeToFilters() {
        String typeName = " com.example.EventType ";
        EventFilter validFilter = filterForType(typeName);
        EventFilter invalidFilter = filterForType("   ");
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .addFilter(validFilter)
                .addFilter(invalidFilter)
                .build();
        TargetFilters entityFilters = convert(query);
        assertEquals(1, entityFilters.getFilterCount());

        CompositeFilter compositeFilter = entityFilters.getFilter(0);
        List<Filter> filters = compositeFilter.getFilterList();
        assertEquals(CompositeOperator.EITHER, compositeFilter.getOperator());
        assertEquals(1, filters.size());
        Any typeNameAsAny = filters.get(0)
                                   .getValue();
        assertEquals(typeName.trim(), toObject(typeNameAsAny, String.class));
    }

    private static EventFilter filterForType(String typeName) {
        return EventFilter.newBuilder()
                          .setEventType(typeName)
                          .build();
    }
}
