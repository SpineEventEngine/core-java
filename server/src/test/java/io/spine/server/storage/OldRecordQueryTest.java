/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.truth.StringSubject;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.client.Filter;
import io.spine.client.IdFilter;
import io.spine.test.entity.ProjectId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.server.entity.storage.given.AColumn.column;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("`RecordQuery` should")
class OldRecordQueryTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(IdFilter.class, IdFilter.getDefaultInstance())
                .setDefault(QueryParameters.class, QueryParameters.newBuilder()
                                                                  .build())
                .testStaticMethods(OldRecordQuery.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    @DisplayName("support `toString`")
    void supportToString() {
        Object someId = Sample.messageOfType(ProjectId.class);
        Collection<Object> ids = singleton(someId);
        OldColumn someColumn = column();
        Object someValue = "something";

        Map<OldColumn, Object> params = new HashMap<>(1);
        params.put(someColumn, someValue);

        OldRecordQuery<?> query = RecordQueries.of(ids, paramsFromValues(params));

        StringSubject assertQuery = assertThat(query.toString());
        assertQuery.contains(query.getIds()
                                  .toString());
        assertQuery.contains(query.getParameters()
                                  .toString());
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        EqualsTester tester = new EqualsTester();
        addEqualityGroupA(tester);
        addEqualityGroupB(tester);
        addEqualityGroupC(tester);
        addEqualityGroupD(tester);
        tester.testEquals();
    }

    /**
     * Adds an equality group containing a single Query of two IDs and two Query parameters to
     * the given {@link EqualsTester}.
     */
    private static void addEqualityGroupA(EqualsTester tester) {
        Collection<?> ids = Arrays.asList(Sample.messageOfType(ProjectId.class), 0);
        Map<OldColumn, Object> params = new IdentityHashMap<>(2);
        params.put(column(), "anything");
        params.put(column(), 5);
        OldRecordQuery<?> query = RecordQueries.of(ids, paramsFromValues(params));
        tester.addEqualityGroup(query);
    }

    /**
     * Adds an equality group containing two Queries with no IDs and with a single Query parameter
     * to the given {@link EqualsTester}.
     */
    private static void addEqualityGroupB(EqualsTester tester) {
        Collection<?> ids = emptyList();
        Map<OldColumn, Object> params = new HashMap<>(1);
        params.put(column(), 5);
        OldRecordQuery<?> query1 = RecordQueries.of(ids, paramsFromValues(params));
        OldRecordQuery<?> query2 = RecordQueries.of(ids, paramsFromValues(params));
        tester.addEqualityGroup(query1, query2);
    }

    /**
     * Adds an equality group containing two Queries with no IDs and with a single Query parameter
     * to the given {@link EqualsTester}.
     *
     * <p>The values of Query parameters are different than in
     * {@linkplain #addEqualityGroupB(EqualsTester) group C}.
     */
    private static void addEqualityGroupC(EqualsTester tester) {
        Collection<?> ids = emptySet();
        OldColumn column = column();
        Object value = 42;
        Map<OldColumn, Object> params = new HashMap<>(1);
        params.put(column, value);
        OldRecordQuery<?> query1 = RecordQueries.of(ids, paramsFromValues(params));
        OldRecordQuery<?> query2 = RecordQueries.of(ids, paramsFromValues(params));
        tester.addEqualityGroup(query1, query2);
    }

    /**
     * Adds an equality group containing a single Query with a single ID and no Query parameters
     * to the given {@link EqualsTester}.
     */
    private static void addEqualityGroupD(EqualsTester tester) {
        Collection<ProjectId> ids = singleton(Sample.messageOfType(ProjectId.class));
        Map<OldColumn, Object> columns = Collections.emptyMap();
        OldRecordQuery<?> query = RecordQueries.of(ids, paramsFromValues(columns));
        tester.addEqualityGroup(query);
    }

    private static QueryParameters paramsFromValues(Map<OldColumn, Object> values) {
        QueryParameters.Builder builder = QueryParameters.newBuilder();
        Multimap<OldColumn, Filter> filters = HashMultimap.create(values.size(), 1);
        for (Map.Entry<OldColumn, Object> param : values.entrySet()) {
            OldColumn column = param.getKey();
            FieldPath fieldPath = Field.parse(column.name()
                                                    .value())
                                       .path();
            Filter filter = Filter
                    .newBuilder()
                    .setOperator(EQUAL)
                    .setValue(toAny(param.getValue()))
                    .setFieldPath(fieldPath)
                    .build();
            filters.put(column, filter);
        }
        return builder.add(CompositeQueryParameter.from(filters, ALL))
                      .build();
    }
}
