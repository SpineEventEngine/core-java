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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertEmpty;
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

    @Test
    public void construct_empty_queries() {
        final EntityFilters filters = EntityFilters.getDefaultInstance();
        final Class<? extends Entity> entityClass = AbstractEntity.class;
        final EntityQuery<?> query = EntityQueries.from(filters, entityClass);
        assertNotNull(query);
        assertEmpty(query.getParameters());
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
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .putColumnFilter("version",
                                                                    AnyPacker.pack(versionValue))
                                                   .putColumnFilter("archived",
                                                                    AnyPacker.pack(archivedValue))
                                                   .build();
        final Class<? extends Entity> entityClass = AbstractVersionableEntity.class;
        final EntityQuery<?> query = EntityQueries.from(filters, entityClass);
        assertNotNull(query);
        assertSize(2, query.getParameters());

        final Collection<?> ids = query.getIds();
        assertFalse(ids.isEmpty());
        assertSize(1, ids);
        final Object singleId = ids.iterator().next();
        assertEquals(someGenericId, singleId);

        final Map<Column<?>, Object> params = query.getParameters();
        final Collection<Object> values = params.values();
        assertContains(versionValue, values);
        assertContains(archivedValue.getValue(), values);
    }
}
