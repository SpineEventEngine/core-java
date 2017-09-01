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

package io.spine.server.entity.storage;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import static io.spine.server.entity.storage.ColumnShould.TestEntity;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static org.mockito.Mockito.mock;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnMemoizedValueShould {

    private static final String MUTABLE_STATE_COLUMN = "mutableState";
    private static final String ARCHIVED_COLUMN = archived.name();

    @Test
    public void be_serializable() {
        final EntityColumn column = findColumn(TestEntity.class, MUTABLE_STATE_COLUMN);
        final TestEntity entity = new TestEntity("my-id");
        entity.setMutableState(42);
        final MemoizedValue value = column.memoizeFor(entity);

        reserializeAndAssert(value);
    }

    @Test
    public void support_equality() {
        /*
        4 equality groups represent following cases:
            A - 3 values of the same column memoized on the same instance of Entity;
            A-mutated - the value of the same column on the same object but after the field change;
            B - the value of a different column on the same instance;
            C - the value of a mock column (used to ensure that MemoizedValue#equals is decoupled
                from the EntityColumn#equals).
         */

        final EntityColumn columnA = findColumn(TestEntity.class, MUTABLE_STATE_COLUMN);
        final EntityColumn columnB = findColumn(TestEntity.class, ARCHIVED_COLUMN);
        final EntityColumn columnC = mock(EntityColumn.class);

        final TestEntity entity = new TestEntity("ID");

        final MemoizedValue valueA1 = columnA.memoizeFor(entity);
        final MemoizedValue valueA2 = columnA.memoizeFor(entity);
        final MemoizedValue valueA3 = columnA.memoizeFor(entity);

        entity.setMutableState(42);

        final MemoizedValue valueAMutated = columnA.memoizeFor(entity);

        final MemoizedValue valueB = columnB.memoizeFor(entity);

        final MemoizedValue valueC = new MemoizedValue(columnC, entity.getState());

        new EqualsTester().addEqualityGroup(valueA1, valueA2, valueA3)
                          .addEqualityGroup(valueAMutated)
                          .addEqualityGroup(valueB)
                          .addEqualityGroup(valueC)
                          .testEquals();
    }
}
