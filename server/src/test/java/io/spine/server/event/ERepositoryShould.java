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

package io.spine.server.event;

import com.google.protobuf.Any;
import com.google.protobuf.util.Timestamps;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.CompositeColumnFilter.CompositeOperator;
import io.spine.client.EntityFilters;
import org.junit.Test;

import java.util.List;

import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.server.event.ERepository.toEntityFilters;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Grankin
 */
public class ERepositoryShould {

    @Test
    public void convert_empty_query_to_empty_EntityFilters() {
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .build();
        final EntityFilters entityFilters = toEntityFilters(query);
        assertTrue(entityFilters.getFilterList()
                                .isEmpty());
    }

    @Test
    public void convert_time_query_to_EntityFilters() {
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(Timestamps.MIN_VALUE)
                                                       .setBefore(Timestamps.MAX_VALUE)
                                                       .build();
        final EntityFilters entityFilters = toEntityFilters(query);
        assertEquals(1, entityFilters.getFilterCount());

        final CompositeColumnFilter compositeFilter = entityFilters.getFilter(0);
        final List<ColumnFilter> columnFilters = compositeFilter.getFilterList();
        assertEquals(CompositeOperator.ALL, compositeFilter.getOperator());
        assertEquals(2, columnFilters.size());
    }

    @Test
    public void convert_type_query_to_EntityFilters() {
        final String typeName = " com.example.EventType ";
        final EventFilter validFilter = filterForType(typeName);
        final EventFilter invalidFilter = filterForType("   ");
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(validFilter)
                                                       .addFilter(invalidFilter)
                                                       .build();
        final EntityFilters entityFilters = toEntityFilters(query);
        assertEquals(1, entityFilters.getFilterCount());

        final CompositeColumnFilter compositeFilter = entityFilters.getFilter(0);
        final List<ColumnFilter> columnFilters = compositeFilter.getFilterList();
        assertEquals(CompositeOperator.EITHER, compositeFilter.getOperator());
        assertEquals(1, columnFilters.size());
        final Any typeNameAsAny = columnFilters.get(0)
                                               .getValue();
        assertEquals(typeName, toObject(typeNameAsAny, String.class));
    }

    private static EventFilter filterForType(String typeName) {
        return EventFilter.newBuilder()
                          .setEventType(typeName)
                          .build();
    }
}
