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

import com.google.common.base.Function;
import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.EntityRecord;
import io.spine.test.Tests;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertContainsAll;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnRecordsShould {

    private static final int MOCK_COLUMNS_COUNT = 3;
    private static final int MOCK_NULL_COLUMNS_COUNT = MOCK_COLUMNS_COUNT / 2;
    private static final int MOCK_NON_NULL_COLUMNS_COUNT =
            MOCK_COLUMNS_COUNT - MOCK_NULL_COLUMNS_COUNT;

    @Test
    @DisplayName("have private util ctor")
    void havePrivateUtilCtor() {
        assertHasPrivateParameterlessCtor(ColumnRecords.class);
    }

    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() {
        final EntityRecordWithColumns record = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance());
        final ColumnTypeRegistry columnTypeRegistry = ColumnTypeRegistry.newBuilder()
                                                                        .build();
        final EntityQuery entityQuery = EntityQuery.of(Collections.emptyList(),
                                                       QueryParameters.newBuilder()
                                                                      .build());
        new NullPointerTester()
                .setDefault(EntityRecordWithColumns.class, record)
                .setDefault(ColumnTypeRegistry.class, columnTypeRegistry)
                .setDefault(EntityQuery.class, entityQuery)
                .testAllPublicStaticMethods(ColumnRecords.class);
    }

    @Test
    @DisplayName("feed entity columns to database record")
    void feedEntityColumnsToDatabaseRecord() {
        // Set up mocks and arguments
        final List<Object> destination = new ArrayList<>(MOCK_COLUMNS_COUNT);

        final Map<String, EntityColumn.MemoizedValue> columns = setupMockColumnsAllowingNulls();

        final CollectAnyColumnType type = spy(CollectAnyColumnType.class);
        final ColumnTypeRegistry<CollectAnyColumnType> registry =
                ColumnTypeRegistry.<CollectAnyColumnType>newBuilder()
                                  .put(Object.class, type)
                                  .build();
        final EntityRecordWithColumns recordWithColumns = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance(), columns);

        final Function<String, Object> colIdMapper = spy(new NoOpColumnIdentifierMapper());

        // Invoke the pre-persistence action
        ColumnRecords.feedColumnsTo(destination, recordWithColumns, registry, colIdMapper);

        // Verify calls
        verify(colIdMapper, times(MOCK_COLUMNS_COUNT)).apply(anyString());
        verify(type, times(MOCK_NON_NULL_COLUMNS_COUNT))
                .setColumnValue(eq(destination), any(Object.class), anyString());
        verify(type, times(MOCK_NULL_COLUMNS_COUNT)).setNull(eq(destination), anyString());

        final int indexOfNull = destination.indexOf(null);
        assertTrue("Null value was not saved to the destination", indexOfNull >= 0);
        assertContainsAll(destination, getNonNullColumnValues().toArray());
    }

    private static Map<String, EntityColumn.MemoizedValue> setupMockColumnsAllowingNulls() {
        final EntityColumn mockColumn = mock(EntityColumn.class);
        when(mockColumn.getType()).thenReturn(Object.class);
        when(mockColumn.getPersistedType()).thenReturn(Object.class);
        final Map<String, EntityColumn.MemoizedValue> columns = new HashMap<>(MOCK_COLUMNS_COUNT);
        for (int i = 0; i < MOCK_COLUMNS_COUNT; i++) {
            final Integer columnValueToPersist = (i % 2 != 0) ? null : i;

            final EntityColumn.MemoizedValue value = mock(EntityColumn.MemoizedValue.class);
            when(value.getSourceColumn()).thenReturn(mockColumn);
            when(value.getValue()).thenReturn(columnValueToPersist);

            columns.put(String.valueOf(i), value);
        }
        return columns;
    }

    private static Collection getNonNullColumnValues() {
        final List<Object> values = new ArrayList<>();
        for (int i = 0; i < MOCK_COLUMNS_COUNT; i += 2) { // each second value is non-null
            values.add(i);
        }
        return values;
    }

    static class CollectAnyColumnType implements ColumnType<Object, Object, Collection<Object>, Object> {
        @Override
        public Object convertColumnValue(Object fieldValue) {
            return fieldValue;
        }

        @Override
        public void setColumnValue(Collection<Object> storageRecord, Object value,
                                   Object columnIdentifier) {
            storageRecord.add(value);
        }

        @Override
        public void setNull(Collection<Object> storageRecord, Object columnIdentifier) {
            storageRecord.add(Tests.nullRef());
        }
    }

    static class NoOpColumnIdentifierMapper implements Function<String, Object> {
        @Override
        public Object apply(@Nullable String s) {
            return s;
        }
    }
}
