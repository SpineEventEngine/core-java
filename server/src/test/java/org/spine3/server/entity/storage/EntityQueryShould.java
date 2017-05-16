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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.spine3.client.EntityIdFilter;
import org.spine3.test.entity.ProjectId;
import org.spine3.testdata.Sample;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.mock;
import static org.spine3.server.entity.storage.QueryParameters.fromValues;
import static org.spine3.test.Verify.assertContains;

/**
 * @author Dmytro Dashenkov
 */
public class EntityQueryShould {

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(EntityIdFilter.class, EntityIdFilter.getDefaultInstance())
                .setDefault(QueryParameters.class, QueryParameters.newBuilder()
                                                                  .build())
                .testStaticMethods(EntityQuery.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void support_equality() {
        final EqualsTester tester = new EqualsTester();
        addEqualityGroupA(tester);
        addEqualityGroupB(tester);
        addEqualityGroupC(tester);
        addEqualityGroupD(tester);
        tester.testEquals();
    }

    private static void addEqualityGroupA(EqualsTester tester) {
        final Collection<?> ids = Arrays.asList(Sample.messageOfType(ProjectId.class), 0);
        final Map<Column<?>, Object> params = new IdentityHashMap<>(2);
        params.put(mock(Column.class), "anything");
        params.put(mock(Column.class), 5);
        final EntityQuery<?> query = EntityQuery.of(ids, fromValues(params));
        tester.addEqualityGroup(query);
    }

    private static void addEqualityGroupB(EqualsTester tester) {
        final Collection<?> ids = emptyList();
        final Map<Column<?>, Object> params = new HashMap<>(1);
        params.put(mock(Column.class), 5);
        final EntityQuery<?> query1 = EntityQuery.of(ids, fromValues(params));
        final EntityQuery<?> query2 = EntityQuery.of(ids, fromValues(params));
        tester.addEqualityGroup(query1, query2);
    }

    private static void addEqualityGroupC(EqualsTester tester) {
        final Collection<?> ids = emptySet();
        final Column<?> column = mock(Column.class);
        final Object value = 42;
        final Map<Column<?>, Object> params1 = new HashMap<>(1);
        params1.put(column, value);
        final Map<Column<?>, Object> params2 = new HashMap<>(1);
        params2.put(column, value);
        final EntityQuery<?> query1 = EntityQuery.of(ids, fromValues(params1));
        final EntityQuery<?> query2 = EntityQuery.of(ids, fromValues(params2));
        tester.addEqualityGroup(query1, query2);
    }

    private static void addEqualityGroupD(EqualsTester tester) {
        final Collection<ProjectId> ids = singleton(Sample.messageOfType(ProjectId.class));
        final Map<Column<?>, Object> columns = Collections.emptyMap();
        final EntityQuery<?> query = EntityQuery.of(ids, fromValues(columns));
        tester.addEqualityGroup(query);
    }

    @Test
    public void support_toString() {
        final Object someId = Sample.messageOfType(ProjectId.class);
        final Collection<Object> ids = singleton(someId);
        final Column<?> someColumn = mock(Column.class);
        final Object someValue = "something";

        final Map<Column<?>, Object> params = new HashMap<>(1);
        params.put(someColumn, someValue);

        final EntityQuery query = EntityQuery.of(ids, fromValues(params));
        final String repr = query.toString();

        assertContains(query.getIds()
                            .toString(), repr);
        assertContains(query.getParameters()
                            .toString(), repr);
    }
}
