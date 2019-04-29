/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.given.ColumnRecordsTestEnv.CollectAnyColumnType;
import io.spine.server.entity.storage.given.ColumnRecordsTestEnv.NoOpColumnIdentifierMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.given.ColumnRecordsTestEnv.MOCK_COLUMNS_COUNT;
import static io.spine.server.entity.storage.given.ColumnRecordsTestEnv.getNonNullColumnValues;
import static io.spine.server.entity.storage.given.ColumnRecordsTestEnv.setupMockColumnsAllowingNulls;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ColumnRecords utility should")
class ColumnRecordsTest {

    private static final int MOCK_NULL_COLUMNS_COUNT = MOCK_COLUMNS_COUNT / 2;
    private static final int MOCK_NON_NULL_COLUMNS_COUNT =
            MOCK_COLUMNS_COUNT - MOCK_NULL_COLUMNS_COUNT;

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ColumnRecords.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        EntityRecordWithColumns record = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance());
        ColumnTypeRegistry columnTypeRegistry = ColumnTypeRegistry.newBuilder()
                                                                  .build();
        EntityQuery entityQuery = EntityQuery.of(Collections.emptyList(),
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
    void feedColumnsToDbRecord() {
        // Set up mocks and arguments
        List<Object> destination = new ArrayList<>(MOCK_COLUMNS_COUNT);

        Map<String, EntityColumn.MemoizedValue> columns = setupMockColumnsAllowingNulls();

        CollectAnyColumnType type = spy(CollectAnyColumnType.class);
        ColumnTypeRegistry<CollectAnyColumnType> registry =
                ColumnTypeRegistry.<CollectAnyColumnType>newBuilder()
                        .put(Object.class, type)
                        .build();
        EntityRecordWithColumns recordWithColumns = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance(), columns);

        Function<String, Object> colIdMapper = spy(new NoOpColumnIdentifierMapper());

        // Invoke the pre-persistence action
        ColumnRecords.feedColumnsTo(destination, recordWithColumns, registry, colIdMapper);

        // Verify calls
        verify(colIdMapper, times(MOCK_COLUMNS_COUNT)).apply(anyString());
        verify(type, times(MOCK_NON_NULL_COLUMNS_COUNT))
                .setColumnValue(eq(destination), any(Object.class), anyString());
        verify(type, times(MOCK_NULL_COLUMNS_COUNT)).setNull(eq(destination), anyString());

        int indexOfNull = destination.indexOf(null);
        assertTrue(indexOfNull >= 0, "Null value was not saved to the destination");
        assertThat(destination).containsAtLeastElementsIn(getNonNullColumnValues());
    }
}
