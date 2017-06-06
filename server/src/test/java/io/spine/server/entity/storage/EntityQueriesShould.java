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

package org.spine3.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.base.Version;
import org.spine3.client.ColumnFilter;
import org.spine3.client.ColumnFilters;
import org.spine3.client.CompositeColumnFilter;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.AbstractEntity;
import org.spine3.server.entity.AbstractVersionableEntity;
import org.spine3.server.entity.Entity;
import org.spine3.test.storage.ProjectId;
import org.spine3.testdata.Sample;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static org.spine3.server.storage.EntityField.version;
import static org.spine3.server.storage.LifecycleFlagField.archived;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertSize;

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
                .testAllPublicStaticMethods(EntityQueries.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_filter_type() {
        // Boolean Column queried for for an Integer value
        final ColumnFilter filter = ColumnFilters.gt(archived.name(), 42);
        final CompositeColumnFilter compositeFilter = ColumnFilters.all(filter);
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(compositeFilter)
                                                   .build();
        EntityQueries.from(filters, AbstractVersionableEntity.class);
    }


    @Test
    public void construct_empty_queries() {
        final EntityFilters filters = EntityFilters.getDefaultInstance();
        final Class<? extends Entity> entityClass = AbstractEntity.class;
        final EntityQuery<?> query = EntityQueries.from(filters, entityClass);
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
        final Class<? extends Entity> entityClass = AbstractVersionableEntity.class;
        final EntityQuery<?> query = EntityQueries.from(filters, entityClass);
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
}
