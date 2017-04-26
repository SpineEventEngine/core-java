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
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.spine3.test.Verify.assertContains;

/**
 * @author Dmytro Dashenkov
 */
public class EntityQueryShould {

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(EntityIdFilter.class, EntityIdFilter.getDefaultInstance())
                .testStaticMethods(EntityQuery.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void support_equality() {
        final Object defaultId = 0;
        final ProjectId someId = Sample.messageOfType(ProjectId.class);
        final ProjectId someIdCopy = ProjectId.newBuilder(someId)
                                              .build();
        final ProjectId otherId = Sample.messageOfType(ProjectId.class);
        final Collection<Object> idsA = Arrays.asList(someId, defaultId);
        final Collection<Object> idsB = Collections.emptyList();
        final Collection<Object> idsC = Arrays.<Object>asList(otherId, someIdCopy);
        final Column<?> someColumn = mock(Column.class);
        final Column<?> otherColumn = mock(Column.class);

        final Object someValue = "anything";
        final Object otherValue = 5;

        final Map<Column<?>, Object> paramsA = new HashMap<>(2);
        paramsA.put(someColumn, someValue);
        paramsA.put(otherColumn, otherValue);

        final Map<Column<?>, Object> paramsB = new HashMap<>(1);
        paramsB.put(someColumn, otherValue);

        final Map<Column<?>, Object> paramsC = new HashMap<>(1);
        paramsA.put(otherColumn, someValue);

        final Map<Column<?>, Object> paramsCCopy = new HashMap<>(1);
        paramsA.put(otherColumn, someValue);

        final EntityQuery queryA = EntityQuery.of(idsA, paramsA);
        final EntityQuery queryB = EntityQuery.of(idsB, paramsB);
        final EntityQuery queryC = EntityQuery.of(idsC, paramsC);
        final EntityQuery queryD = EntityQuery.of(idsA, paramsC);
        final EntityQuery queryE = EntityQuery.of(idsB, paramsB);
        final EntityQuery queryF = EntityQuery.of(idsC, paramsCCopy);

        new EqualsTester()
                .addEqualityGroup(queryA)
                .addEqualityGroup(queryB, queryE)
                .addEqualityGroup(queryC, queryF)
                .addEqualityGroup(queryD)
                .testEquals();
    }

    @Test
    public void support_toString() {
        final Object someId = Sample.messageOfType(ProjectId.class);
        final Collection<Object> ids = Collections.singleton(someId);
        final Column<?> someColumn = mock(Column.class);
        final Object someValue = "something";

        final Map<Column<?>, Object> params = new HashMap<>(1);
        params.put(someColumn, someValue);

        final EntityQuery query = EntityQuery.of(ids, params);
        final String repr = query.toString();

        assertContains(query.getIds()
                            .toString(), repr);
        assertContains(query.getParameters()
                            .toString(), repr);
    }
}
