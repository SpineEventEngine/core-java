/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.client.IdFilter;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.client.TargetFilters;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.given.TestEntity;
import io.spine.server.entity.storage.given.TestProjection;
import io.spine.server.storage.LifecycleFlagField;
import io.spine.server.storage.RecordStorage;
import io.spine.test.storage.ProjectId;
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
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.server.entity.storage.given.EntityQueriesTestEnv.order;
import static io.spine.server.entity.storage.given.EntityQueriesTestEnv.pagination;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("EntityQueries utility should")
class EntityQueriesTest extends UtilityClassTest<EntityQueries> {

    private EntityQueriesTest() {
        super(EntityQueries.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(OrderBy.class, OrderBy.getDefaultInstance())
              .setDefault(Pagination.class, Pagination.getDefaultInstance())
              .setDefault(TargetFilters.class, TargetFilters.getDefaultInstance())
              .setDefault(RecordStorage.class, mock(RecordStorage.class))
              .testStaticMethods(getUtilityClass(), NullPointerTester.Visibility.PACKAGE);
    }

    private static EntityQuery<?> createEntityQuery(TargetFilters filters,
                                                    Class<? extends Entity<?, ?>> entityClass) {
        return createEntityQuery(filters, OrderBy.getDefaultInstance(),
                                 Pagination.getDefaultInstance(), entityClass);
    }

    /**
     * This method is not placed in test environment because it uses package-private
     * {@link EntityQueries#from(EntityFilters, OrderBy, Pagination, Collection)}.
     */
    private static EntityQuery<?> createEntityQuery(TargetFilters filters,
                                                    OrderBy orderBy,
                                                    Pagination pagination,
                                                    Class<? extends Entity<?, ?>> entityClass) {
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entityClass);
        return EntityQueries.from(filters, orderBy, pagination, entityColumns);
    }

    @Test
    @DisplayName("check filter type")
    void checkFilterType() {
        // Boolean EntityColumn queried for for an Integer value
        Filter filter = Filters.gt(archived.name(), 42);
        CompositeFilter compositeFilter = Filters.all(filter);
        TargetFilters filters = TargetFilters
                .vBuilder()
                .addFilter(compositeFilter)
                .build();

        assertThrows(IllegalArgumentException.class,
                     () -> createEntityQuery(filters, TestEntity.class));
    }

    @Test
    @DisplayName("not create query for non-existing column")
    void notCreateForNonExisting() {
        Filter filter = Filters.eq("nonExistingColumn", 42);
        CompositeFilter compositeFilter = Filters.all(filter);
        TargetFilters filters = TargetFilters
                .vBuilder()
                .addFilter(compositeFilter)
                .build();

        assertThrows(IllegalArgumentException.class,
                     () -> createEntityQuery(filters, TestEntity.class));
    }

    @Test
    @DisplayName("construct empty queries")
    void constructEmptyQueries() {
        TargetFilters filters = TargetFilters.getDefaultInstance();
        EntityQuery<?> query = createEntityQuery(filters, TestEntity.class);
        assertNotNull(query);

        assertTrue(query.getIds()
                        .isEmpty());

        QueryParameters parameters = query.getParameters();
        assertEquals(0, size(parameters.iterator()));
        assertFalse(parameters.limited());
        assertFalse(parameters.ordered());
    }

    @Test
    @DisplayName("construct non-empty queries")
    void constructNonEmptyQueries() {
        Message someGenericId = Sample.messageOfType(ProjectId.class);
        Any entityId = AnyPacker.pack(someGenericId);
        IdFilter idFilter = IdFilter
                .vBuilder()
                .addIds(entityId)
                .build();
        BoolValue archived = BoolValue
                .newBuilder()
                .setValue(true)
                .build();
        Filter archivedFilter = Filters
                .eq(LifecycleFlagField.archived.name(), archived);
        CompositeFilter aggregatingFilter = CompositeFilter
                .vBuilder()
                .addFilter(archivedFilter)
                .setOperator(EITHER)
                .build();
        TargetFilters filters = TargetFilters
                .vBuilder()
                .setIdFilter(idFilter)
                .addFilter(aggregatingFilter)
                .build();
        EntityQuery<?> query = createEntityQuery(filters, TestProjection.class);
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

        assertFalse(parameters.limited());
        assertFalse(parameters.ordered());

        CompositeQueryParameter singleParam = values.get(0);
        Collection<Filter> columnFilters = singleParam.getFilters()
                                                      .values();
        assertEquals(EITHER, singleParam.getOperator());
        IterableSubject assertColumnFilters = assertThat(columnFilters);
        assertColumnFilters.contains(archivedFilter);
    }

    @Test
    @DisplayName("construct queries with limit and order")
    void constructWithLimitAndOrder() {
        String expectedColumn = "test";
        OrderBy.Direction expectedDirection = ASCENDING;
        int expectedLimit = 10;
        EntityQuery<?> query = createEntityQuery(TargetFilters.getDefaultInstance(),
                                                 order(expectedColumn, expectedDirection),
                                                 pagination(expectedLimit),
                                                 TestEntity.class);
        assertNotNull(query);

        QueryParameters parameters = query.getParameters();
        assertTrue(parameters.ordered());
        assertTrue(parameters.limited());

        OrderBy orderBy = parameters.orderBy();
        assertEquals(expectedColumn, orderBy.getColumn());
        assertEquals(expectedDirection, orderBy.getDirection());

        assertEquals(expectedLimit, parameters.limit());
    }
}
