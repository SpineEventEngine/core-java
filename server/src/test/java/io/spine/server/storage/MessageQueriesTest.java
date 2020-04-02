/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.client.IdFilter;
import io.spine.client.TargetFilters;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.EntityColumns;
import io.spine.server.entity.storage.LifecycleColumn;
import io.spine.server.entity.storage.given.TestEntity;
import io.spine.server.entity.storage.given.TestProjection;
import io.spine.test.entity.ProjectId;
import io.spine.testdata.Sample;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.server.entity.storage.LifecycleColumn.archived;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`MessageQueries` utility should")
//TODO:2020-04-01:alex.tymchenko: add the tests for the rest of the factory methods.
class MessageQueriesTest extends UtilityClassTest<MessageQueries> {

    private MessageQueriesTest() {
        super(MessageQueries.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);

        tester.setDefault(TargetFilters.class, TargetFilters.getDefaultInstance())
              .setDefault(QueryParameters.class, QueryParameters.newBuilder()
                                                                .build())
              .setDefault(MessageColumn.class, sampleColumn())
              .setDefault(Columns.class, MessageColumns.emptyOf(Any.class))
              .testStaticMethods(getUtilityClass(), NullPointerTester.Visibility.PACKAGE);
    }

    private static MessageColumn<String, Any> sampleColumn() {
        return new MessageColumn<>(ColumnName.of("sample"), String.class, (v) -> "");
    }

    @Test
    @DisplayName("check filter type")
    void checkFilterType() {
        // `Boolean` column is queried for an Integer value.
        Filter filter = Filters.gt(archived.name(), 42);
        CompositeFilter compositeFilter = Filters.all(filter);
        TargetFilters filters = TargetFilters
                .newBuilder()
                .addFilter(compositeFilter)
                .build();

        EntityColumns columns = EntityColumns.of(TestEntity.class);

        assertThrows(IllegalArgumentException.class,
                     () -> MessageQueries.from(filters, columns));
    }

    @Test
    @DisplayName("not create query for non-existing column")
    void notCreateForNonExisting() {
        // `Boolean` column is queried for an Integer value.
        Filter filter = Filters.gt("column that does not exist", 42);
        CompositeFilter compositeFilter = Filters.all(filter);
        TargetFilters filters = TargetFilters
                .newBuilder()
                .addFilter(compositeFilter)
                .build();

        EntityColumns columns = EntityColumns.of(TestEntity.class);

        assertThrows(IllegalArgumentException.class,
                     () -> MessageQueries.from(filters, columns));
    }

    @Test
    @DisplayName("construct empty queries")
    void constructEmptyQueries() {
        TargetFilters filters = TargetFilters.getDefaultInstance();
        EntityColumns columns = EntityColumns.of(TestEntity.class);
        MessageQuery<?> query = MessageQueries.from(filters, columns);
        assertNotNull(query);

        assertTrue(query.getIds()
                        .isEmpty());

        QueryParameters parameters = query.getParameters();
        assertEquals(0, size(parameters.iterator()));
    }

    @Test
    @DisplayName("construct non-empty queries")
    void constructNonEmptyQueries() {
        Message someGenericId = Sample.messageOfType(ProjectId.class);
        Any entityId = Identifier.pack(someGenericId);
        IdFilter idFilter = IdFilter
                .newBuilder()
                .addId(entityId)
                .build();
        BoolValue archived = BoolValue
                .newBuilder()
                .setValue(true)
                .build();
        Filter archivedFilter = Filters
                .eq(LifecycleColumn.archived.name(), archived);
        CompositeFilter aggregatingFilter = CompositeFilter
                .newBuilder()
                .addFilter(archivedFilter)
                .setOperator(EITHER)
                .build();
        TargetFilters filters = TargetFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .addFilter(aggregatingFilter)
                .build();
        EntityColumns columns = EntityColumns.of(TestProjection.class);
        MessageQuery<?> query = MessageQueries.from(filters, columns);
        assertNotNull(query);

        Collection<?> ids = query.getIds();
        assertFalse(ids.isEmpty());
        assertThat(ids).hasSize(1);
        Object singleId = ids.iterator()
                             .next();
        assertEquals(someGenericId, singleId);

        QueryParameters parameters = query.getParameters();

        List<CompositeQueryParameter> values = newArrayList(parameters);
        assertThat(values).hasSize(1);

        CompositeQueryParameter singleParam = values.get(0);
        Collection<Filter> columnFilters = singleParam.filters()
                                                      .values();
        assertEquals(EITHER, singleParam.operator());
        IterableSubject assertColumnFilters = assertThat(columnFilters);
        assertColumnFilters.contains(archivedFilter);
    }
}
