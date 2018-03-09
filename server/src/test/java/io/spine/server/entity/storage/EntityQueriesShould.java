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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
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
import io.spine.test.storage.ProjectId;
import io.spine.testdata.Sample;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
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

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(EntityQueries.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(EntityFilters.class, EntityFilters.getDefaultInstance())
                .setDefault(EntityColumnCache.class, EntityColumnCache.getEmptyInstance())
                .testAllPublicStaticMethods(EntityQueries.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_filter_type() {
        // Boolean EntityColumn queried for for an Integer value
        final ColumnFilter filter = ColumnFilters.gt(archived.name(), 42);
        final CompositeColumnFilter compositeFilter = ColumnFilters.all(filter);
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(compositeFilter)
                                                   .build();
        createEntityQuery(filters, AbstractVersionableEntity.class);
    }


    @Test
    public void construct_empty_queries() {
        final EntityFilters filters = EntityFilters.getDefaultInstance();
        final EntityQuery<?> query = createEntityQuery(filters, AbstractEntity.class);
        assertNotNull(query);
        assertEquals(0, size(query.getParameters().iterator()));
        assertTrue(query.getIds().isEmpty());
    }

    @Test
    public void construct_non_empty_queries() {
        final Message someGenericId = Sample.messageOfType(ProjectId.class);
        final Any someId = AnyPacker.pack(someGenericId);
        final EntityId entityId = EntityId.newBuilder()
                                          .setId(someId)
                                          .build();
        final EntityIdFilter idFilter = EntityIdFilter.newBuilder()
                                                      .addIds(entityId)
                                                      .build();
        final Version versionValue = Version.newBuilder()
                                             .setNumber(1)
                                             .build();
        final BoolValue archivedValue = BoolValue.newBuilder()
                                                 .setValue(true)
                                                 .build();
        final ColumnFilter versionFilter = ColumnFilters.eq(version.name(), versionValue);
        final ColumnFilter archivedFilter = ColumnFilters.eq(archived.name(), archivedValue);
        final CompositeColumnFilter aggregatingFilter =
                CompositeColumnFilter.newBuilder()
                                       .addFilter(versionFilter)
                                       .addFilter(archivedFilter)
                                       .setOperator(EITHER)
                                       .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .addFilter(aggregatingFilter)
                                                   .build();
        final EntityQuery<?> query = createEntityQuery(filters, AbstractVersionableEntity.class);
        assertNotNull(query);

        final Collection<?> ids = query.getIds();
        assertFalse(ids.isEmpty());
        assertSize(1, ids);
        final Object singleId = ids.iterator().next();
        assertEquals(someGenericId, singleId);

        final List<CompositeQueryParameter> values = newArrayList(query.getParameters());
        assertSize(1, values);
        final CompositeQueryParameter singleParam = values.get(0);
        final Collection<ColumnFilter> columnFilters = singleParam.getFilters()
                                                                  .values();
        assertEquals(EITHER, singleParam.getOperator());
        assertContains(versionFilter, columnFilters);
        assertContains(archivedFilter, columnFilters);
    }

    private static EntityQuery<?> createEntityQuery(EntityFilters filters, Class<? extends Entity> entityClass) {
        final EntityColumnCache columnCache = EntityColumnCache.initializeFor(entityClass);
        return EntityQueries.from(filters, columnCache);
    }
}
