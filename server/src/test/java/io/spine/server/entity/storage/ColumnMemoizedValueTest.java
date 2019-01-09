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

import com.google.common.testing.EqualsTester;
import io.spine.server.entity.storage.given.column.TestEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Column MemoizedValue should")
class ColumnMemoizedValueTest {

    private static final String MUTABLE_STATE_COLUMN = "mutableState";
    private static final String ARCHIVED_COLUMN = archived.name();

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        EntityColumn column = findColumn(TestEntity.class, MUTABLE_STATE_COLUMN);
        TestEntity entity = new TestEntity("my-id");
        entity.setMutableState(42);
        MemoizedValue value = column.memoizeFor(entity);

        reserializeAndAssert(value);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        /*
        4 equality groups represent following cases:
            A - 3 values of the same column memoized on the same instance of Entity;
            A-mutated - the value of the same column on the same object but after the field change;
            B - the value of a different column on the same instance;
            C - the value of a mock column (used to ensure that MemoizedValue#equals is decoupled
                from the EntityColumn#equals).
         */

        EntityColumn columnA = findColumn(TestEntity.class, MUTABLE_STATE_COLUMN);
        EntityColumn columnB = findColumn(TestEntity.class, ARCHIVED_COLUMN);
        EntityColumn columnC = mock(EntityColumn.class);

        TestEntity entity = new TestEntity("ID");

        MemoizedValue valueA1 = columnA.memoizeFor(entity);
        MemoizedValue valueA2 = columnA.memoizeFor(entity);
        MemoizedValue valueA3 = columnA.memoizeFor(entity);

        entity.setMutableState(42);

        MemoizedValue valueAMutated = columnA.memoizeFor(entity);

        MemoizedValue valueB = columnB.memoizeFor(entity);

        MemoizedValue valueC = new MemoizedValue(columnC, entity.getState());

        new EqualsTester().addEqualityGroup(valueA1, valueA2, valueA3)
                          .addEqualityGroup(valueAMutated)
                          .addEqualityGroup(valueB)
                          .addEqualityGroup(valueC)
                          .testEquals();
    }

    @Test
    @DisplayName("support comparison")
    void supportComparison() {
        EntityColumn column = findColumn(TestEntity.class, MUTABLE_STATE_COLUMN);

        TestEntity nullEntity = new TestEntity("null", null);
        TestEntity negFirstEntity = new TestEntity("negative-first", -1);
        TestEntity zeroEntity = new TestEntity("zero", 0);
        TestEntity firstEntity = new TestEntity("first", 1);
        TestEntity secondEntity = new TestEntity("second", 2);
        TestEntity thirdEntity = new TestEntity("third", 3);

        MemoizedValue firstValue = column.memoizeFor(firstEntity);
        MemoizedValue negFirstValue = column.memoizeFor(negFirstEntity);
        MemoizedValue zeroValue = column.memoizeFor(zeroEntity);
        MemoizedValue secondValue = column.memoizeFor(secondEntity);
        MemoizedValue thirdValue = column.memoizeFor(thirdEntity);
        MemoizedValue nullValue = column.memoizeFor(nullEntity);

        List<MemoizedValue> values = newArrayList(thirdValue, zeroValue, secondValue, negFirstValue,
                                                  nullValue, firstValue);

        List<MemoizedValue> expected = newArrayList(nullValue, negFirstValue, zeroValue, firstValue,
                                                    secondValue, thirdValue);
        List<MemoizedValue> actual = values.stream()
                                           .sorted()
                                           .collect(toList());
        assertEquals(expected, actual);
    }
}
