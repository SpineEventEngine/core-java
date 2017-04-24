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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import org.junit.Test;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.entity.ProjectId;
import org.spine3.testdata.Sample;

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
        final EntityId defaultId = EntityId.getDefaultInstance();
        final Any someIdWrapped = AnyPacker.pack(Sample.messageOfType(ProjectId.class));
        final EntityId someId = EntityId.newBuilder()
                                        .setId(someIdWrapped)
                                        .build();
        final EntityId someIdCopy = EntityId.newBuilder(someId)
                                            .build();
        final Any otherIdWrapped = AnyPacker.pack(Sample.messageOfType(ProjectId.class));
        final EntityId otherId = EntityId.newBuilder()
                                         .setId(otherIdWrapped)
                                         .build();
        final EntityIdFilter idFilterA = EntityIdFilter.newBuilder()
                                                       .addIds(someId)
                                                       .addIds(defaultId)
                                                       .build();
        final EntityIdFilter idFilterB = EntityIdFilter.getDefaultInstance();
        final EntityIdFilter idFilterC = EntityIdFilter.newBuilder()
                                                       .addIds(otherId)
                                                       .addIds(someIdCopy)
                                                       .build();
        final Column<?> someColumn = mock(Column.class);
        final Column<?> otherColumn = mock(Column.class);

        final Object someValue = "anything";
        final Object otherValue = 5;

        final Multimap<Column<?>, Object> paramsA = HashMultimap.create();
        paramsA.put(someColumn, someValue);
        paramsA.put(otherColumn, otherValue);

        final Multimap<Column<?>, Object> paramsB = HashMultimap.create();
        paramsB.put(someColumn, otherValue);
        paramsB.put(someColumn, otherValue);

        final Multimap<Column<?>, Object> paramsC = HashMultimap.create();
        paramsA.put(otherColumn, someValue);

        final Multimap<Column<?>, Object> paramsCCopy = HashMultimap.create();
        paramsA.put(otherColumn, someValue);

        final EntityQuery queryA = EntityQuery.of(idFilterA, paramsA);
        final EntityQuery queryB = EntityQuery.of(idFilterB, paramsB);
        final EntityQuery queryC = EntityQuery.of(idFilterC, paramsC);
        final EntityQuery queryD = EntityQuery.of(idFilterA, paramsC);
        final EntityQuery queryE = EntityQuery.of(idFilterB, paramsB);
        final EntityQuery queryF = EntityQuery.of(idFilterC, paramsCCopy);

        new EqualsTester()
                .addEqualityGroup(queryA)
                .addEqualityGroup(queryB, queryE)
                .addEqualityGroup(queryC, queryF)
                .addEqualityGroup(queryD)
                .testEquals();
    }

    @Test
    public void support_toString() {
        final Any someIdWrapped = AnyPacker.pack(Sample.messageOfType(ProjectId.class));
        final EntityId someId = EntityId.newBuilder()
                                        .setId(someIdWrapped)
                                        .build();
        final EntityIdFilter idFilter = EntityIdFilter.newBuilder()
                                                      .addIds(someId)
                                                      .build();
        final Column<?> someColumn = mock(Column.class);
        final Object someValue = "something";

        final Multimap<Column<?>, Object> params = HashMultimap.create();
        params.put(someColumn, someValue);

        final EntityQuery query = EntityQuery.of(idFilter, params);
        final String repr = query.toString();

        assertContains(query.getIdFilter().toString(), repr);
        assertContains(query.getParameters().toString(), repr);
    }
}
