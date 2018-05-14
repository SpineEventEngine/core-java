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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import io.spine.client.ColumnFilter;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import org.junit.Test;

import static com.google.common.collect.ImmutableMultimap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.ge;
import static io.spine.client.ColumnFilters.lt;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.CCF_CO_UNDEFINED;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.CompositeQueryParameter.from;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.test.Verify.assertContainsAll;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
public class CompositeQueryParameterShould {

    @Test
    public void not_accept_nulls_on_construction() {
        new NullPointerTester()
                .testStaticMethods(CompositeQueryParameter.class, PACKAGE);
    }

    @Test
    public void be_serializable() {
        final ImmutableMultimap<EntityColumn, ColumnFilter> filters = ImmutableMultimap.of();
        final CompositeQueryParameter parameter = from(filters, ALL);
        SerializableTester.reserializeAndAssert(parameter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_construct_for_invalid_operator() {
        from(ImmutableMultimap.<EntityColumn, ColumnFilter>of(), CCF_CO_UNDEFINED);
    }

    @Test
    public void merge_with_other_instances() {
        final Class<? extends Entity> cls = AbstractVersionableEntity.class;

        final String archivedColumnName = archived.name();
        final String deletedColumnName = deleted.name();
        final String versionColumnName = version.name();

        final EntityColumn archivedColumn = findColumn(cls, archivedColumnName);
        final EntityColumn deletedColumn = findColumn(cls, deletedColumnName);
        final EntityColumn versionColumn = findColumn(cls, versionColumnName);

        final ColumnFilter archived = eq(archivedColumnName, true);
        final ColumnFilter deleted = eq(archivedColumnName, false);
        final ColumnFilter versionLower = ge(archivedColumnName, 2);
        final ColumnFilter versionUpper = lt(archivedColumnName, 10);

        final CompositeQueryParameter lifecycle = from(of(archivedColumn, archived,
                                                          deletedColumn, deleted),
                                                       ALL);
        final CompositeQueryParameter versionLowerBound = from(of(versionColumn, versionLower),
                                                               ALL);
        final CompositeQueryParameter versionUpperBound = from(of(versionColumn, versionUpper),
                                                               ALL);
        // Merge the instances
        final CompositeQueryParameter all = lifecycle.conjunct(newArrayList(versionLowerBound,
                                                                            versionUpperBound));

        // Check
        assertEquals(all.getOperator(), ALL);

        final Multimap<EntityColumn, ColumnFilter> asMultimp = all.getFilters();
        assertContainsAll(asMultimp.get(versionColumn), versionLower, versionUpper);
        assertContainsAll(asMultimp.get(archivedColumn), archived);
        assertContainsAll(asMultimp.get(deletedColumn), deleted);
    }

    @Test
    public void merge_with_single_filter() {
        final Class<? extends Entity> cls = AbstractVersionableEntity.class;

        final String archivedColumnName = archived.name();
        final String deletedColumnName = deleted.name();
        final String versionColumnName = version.name();

        final EntityColumn archivedColumn = findColumn(cls, archivedColumnName);
        final EntityColumn deletedColumn = findColumn(cls, deletedColumnName);
        final EntityColumn versionColumn = findColumn(cls, versionColumnName);

        final ColumnFilter archived = eq(archivedColumnName, false);
        final ColumnFilter deleted = eq(archivedColumnName, false);
        final ColumnFilter version = ge(archivedColumnName, 4);

        final CompositeQueryParameter lifecycle = from(of(archivedColumn, archived,
                                                          deletedColumn, deleted),
                                                       ALL);
        // Merge the instances
        final CompositeQueryParameter all = lifecycle.and(versionColumn, version);

        // Check
        assertEquals(all.getOperator(), ALL);

        final Multimap<EntityColumn, ColumnFilter> asMultimp = all.getFilters();
        assertContainsAll(asMultimp.get(versionColumn), version);
        assertContainsAll(asMultimp.get(archivedColumn), archived);
        assertContainsAll(asMultimp.get(deletedColumn), deleted);
    }
}
