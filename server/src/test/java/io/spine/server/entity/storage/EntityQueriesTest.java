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

package io.spine.server.entity.storage;

import com.google.common.truth.IterableSubject;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import io.spine.client.ColumnFilter;
import io.spine.client.ColumnFilters;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import io.spine.server.storage.LifecycleFlagField;
import io.spine.server.storage.RecordStorage;
import io.spine.test.storage.ProjectId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.server.entity.storage.EntityQueries.from;
import static io.spine.server.entity.storage.given.EntityQueriesTestEnv.order;
import static io.spine.server.entity.storage.given.EntityQueriesTestEnv.pagination;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.testing.Verify.assertContains;
import static io.spine.testing.Verify.assertSize;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EntityQueries utility should")
class EntityQueriesTest {

    private static EntityQuery<?> createEntityQuery(EntityFilters filters,
                                                    Class<? extends Entity> entityClass) {
        return createEntityQuery(filters, OrderBy.getDefaultInstance(),
                                 Pagination.getDefaultInstance(), entityClass);
    }

    /**
     * This method is not placed in test environment because it uses package-private 
     * {@link EntityQueries#from(EntityFilters, OrderBy, Pagination, Collection<EntityColumn>)}.
     */
    private static EntityQuery<?> createEntityQuery(EntityFilters filters,
                                                    OrderBy orderBy,
                                                    Pagination pagination,
                                                    Class<? extends Entity> entityClass) {
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entityClass);
        return from(filters, orderBy, pagination, entityColumns);
    }

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(EntityQueries.class);
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @SuppressWarnings("ConstantConditions")
        // The purpose of the check is passing null for @NotNull field.
        @Test
        @DisplayName("filters")
        void filters() {
            assertThrows(NullPointerException.class,
                         () -> from(null, OrderBy.getDefaultInstance(),
                                    Pagination.getDefaultInstance(), emptyList()));
        }

        @SuppressWarnings("ConstantConditions")
        // The purpose of the check is passing null for @NotNull field.
        @Test
        @DisplayName("storage")
        void storage() {
            RecordStorage<?> storage = null;
            assertThrows(NullPointerException.class,
                         () -> from(EntityFilters.getDefaultInstance(), OrderBy.getDefaultInstance(),
                                    Pagination.getDefaultInstance(), storage));
        }

        @SuppressWarnings("ConstantConditions")
        // The purpose of the check is passing null for @NotNull field.
        @Test
        @DisplayName("order")
        void order() {
            assertThrows(NullPointerException.class,
                         () -> from(EntityFilters.getDefaultInstance(), null,
                                    Pagination.getDefaultInstance(), emptyList()));
        }

        @SuppressWarnings("ConstantConditions")
        // The purpose of the check is passing null for @NotNull field.
        @Test
        @DisplayName("pagination")
        void pagination() {
            assertThrows(NullPointerException.class,
                         () -> from(EntityFilters.getDefaultInstance(), OrderBy.getDefaultInstance(),
                                    null, emptyList()));
        }

        @SuppressWarnings("ConstantConditions")
        // The purpose of the check is passing null for @NotNull field.
        @Test
        @DisplayName("entity class")
        void entityClass() {
            assertThrows(NullPointerException.class,
                         () -> from(EntityFilters.getDefaultInstance(), OrderBy.getDefaultInstance(),
                                    null, emptyList()));
        }
    }

    @Test
    @DisplayName("check filter type")
    void checkFilterType() {
        // Boolean EntityColumn queried for for an Integer value
        ColumnFilter filter = ColumnFilters.gt(archived.name(), 42);
        CompositeColumnFilter compositeFilter = ColumnFilters.all(filter);
        EntityFilters filters =
                EntityFilters.newBuilder()
                             .addFilter(compositeFilter)
                             .build();

        assertThrows(IllegalArgumentException.class,
                     () -> createEntityQuery(filters, AbstractVersionableEntity.class));
    }

    @Test
    @DisplayName("not create query for non-existing column")
    void notCreateForNonExisting() {
        ColumnFilter filter = ColumnFilters.eq("nonExistingColumn", 42);
        CompositeColumnFilter compositeFilter = ColumnFilters.all(filter);
        EntityFilters filters =
                EntityFilters.newBuilder()
                             .addFilter(compositeFilter)
                             .build();

        assertThrows(IllegalArgumentException.class,
                     () -> createEntityQuery(filters, AbstractVersionableEntity.class));
    }

    @Test
    @DisplayName("construct empty queries")
    void constructEmptyQueries() {
        EntityFilters filters = EntityFilters.getDefaultInstance();
        EntityQuery<?> query = createEntityQuery(filters, AbstractEntity.class);
        assertNotNull(query);
        
        assertTrue(query.getIds().isEmpty());
        
        QueryParameters parameters = query.getParameters();
        assertEquals(0, size(parameters.iterator()));
        assertFalse(parameters.limited());
        assertFalse(parameters.ordered());
    }

    @Test
    @DisplayName("construct non-empty queries")
    void constructNonEmptyQueries() {
        Message someGenericId = Sample.messageOfType(ProjectId.class);
        Any someId = AnyPacker.pack(someGenericId);
        EntityId entityId = EntityId
                .newBuilder()
                .setId(someId)
                .build();
        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(entityId)
                .build();
        Version v1 = Version
                .newBuilder()
                .setNumber(1)
                .build();
        BoolValue archived = BoolValue
                .newBuilder()
                .setValue(true)
                .build();
        ColumnFilter versionFilter = ColumnFilters
                .eq(version.name(), v1);
        ColumnFilter archivedFilter = ColumnFilters
                .eq(LifecycleFlagField.archived.name(), archived);
        CompositeColumnFilter aggregatingFilter = CompositeColumnFilter
                .newBuilder()
                .addFilter(versionFilter)
                .addFilter(archivedFilter)
                .setOperator(EITHER)
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .addFilter(aggregatingFilter)
                .build();
        EntityQuery<?> query = createEntityQuery(filters, AbstractVersionableEntity.class);
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
        Collection<ColumnFilter> columnFilters = singleParam.getFilters()
                                                            .values();
        assertEquals(EITHER, singleParam.getOperator());
        IterableSubject assertColumFilters = assertThat(columnFilters);
        assertColumFilters.contains(versionFilter);
        assertColumFilters.contains(archivedFilter);
    }

    @Test
    @DisplayName("construct queries with limit and order")
    void constructWithLimitAndOrder() {
        String expectedColumn = "test";
        OrderBy.Direction expectedDirection = ASCENDING;
        int expectedLimit = 10;
        EntityQuery<?> query = createEntityQuery(EntityFilters.getDefaultInstance(),
                                                 order(expectedColumn, expectedDirection),
                                                 pagination(expectedLimit),
                                                 AbstractEntity.class);
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
