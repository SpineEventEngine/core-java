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

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import io.spine.client.ColumnFilter;
import io.spine.client.ColumnFilters;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import io.spine.server.storage.LifecycleFlagField;
import io.spine.server.storage.RecordStorage;
import io.spine.test.storage.ProjectId;
import io.spine.testdata.Sample;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.server.entity.storage.EntityQueries.from;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertContains;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class EntityQueriesShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static EntityQuery<?> createEntityQuery(EntityFilters filters,
                                                    Class<? extends Entity> entityClass) {
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entityClass);
        return from(filters, entityColumns);
    }

    @Test
    @DisplayName("have private utility ctor")
    void havePrivateUtilityCtor() {
        assertHasPrivateParameterlessCtor(EntityQueries.class);
    }

    @SuppressWarnings("ConstantConditions")
    // The purpose of the check is passing null for @NotNull field.
    @Test
    @DisplayName("not accept null filters")
    void notAcceptNullFilters() {
        thrown.expect(NullPointerException.class);
        from(null, Collections.emptyList());
    }

    @SuppressWarnings("ConstantConditions")
    // The purpose of the check is passing null for @NotNull field.
    @Test
    @DisplayName("not accept null storage")
    void notAcceptNullStorage() {
        RecordStorage<?> storage = null;
        thrown.expect(NullPointerException.class);
        from(EntityFilters.getDefaultInstance(), storage);
    }

    @SuppressWarnings("ConstantConditions")
    // The purpose of the check is passing null for @NotNull field.
    @Test
    @DisplayName("not accept null entity class")
    void notAcceptNullEntityClass() {
        Collection<EntityColumn> entityColumns = null;
        thrown.expect(NullPointerException.class);
        from(EntityFilters.getDefaultInstance(), entityColumns);
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

        thrown.expect(IllegalArgumentException.class);
        createEntityQuery(filters, AbstractVersionableEntity.class);
    }

    @Test
    @DisplayName("not create query for non existing column")
    void notCreateQueryForNonExistingColumn() {
        ColumnFilter filter = ColumnFilters.eq("nonExistingColumn", 42);
        CompositeColumnFilter compositeFilter = ColumnFilters.all(filter);
        EntityFilters filters =
                EntityFilters.newBuilder()
                             .addFilter(compositeFilter)
                             .build();

        thrown.expect(IllegalArgumentException.class);
        createEntityQuery(filters, AbstractVersionableEntity.class);
    }

    @Test
    @DisplayName("construct empty queries")
    void constructEmptyQueries() {
        EntityFilters filters = EntityFilters.getDefaultInstance();
        EntityQuery<?> query = createEntityQuery(filters, AbstractEntity.class);
        assertNotNull(query);
        assertEquals(0, size(query.getParameters()
                                  .iterator()));
        assertTrue(query.getIds()
                        .isEmpty());
    }

    @Test
    @DisplayName("construct non empty queries")
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
        assertSize(1, ids);
        Object singleId = ids.iterator()
                             .next();
        assertEquals(someGenericId, singleId);

        List<CompositeQueryParameter> values = newArrayList(query.getParameters());
        assertSize(1, values);
        CompositeQueryParameter singleParam = values.get(0);
        Collection<ColumnFilter> columnFilters = singleParam.getFilters()
                                                            .values();
        assertEquals(EITHER, singleParam.getOperator());
        assertContains(versionFilter, columnFilters);
        assertContains(archivedFilter, columnFilters);
    }
}
