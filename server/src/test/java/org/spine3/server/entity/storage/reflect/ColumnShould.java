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

package org.spine3.server.entity.storage.reflect;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Any;
import org.junit.Test;
import org.spine3.server.entity.AbstractVersionableEntity;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.storage.Column;
import org.spine3.test.Given;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Many string literals for method names
public class ColumnShould {

    @Test(expected = IllegalArgumentException.class)
    public void costruct_for_getter_method_only() {
        forMethod("toString", Object.class);
    }

    @Test
    public void invoke_getter() {
        final String entityId = "entity-id";
        final Column<String> column = forMethod("getId", Entity.class);
        final TestEntity entity = Given.entityOfClass(TestEntity.class)
                                       .withId(entityId)
                                       .build();
        final String actualId = column.getFor(entity);
        assertEquals(entityId, actualId);
    }

    @Test
    public void have_equals_and_hashCode() {
        final Column<?> col1 = forMethod("getId", Entity.class);
        final Column<?> col2 = forMethod("getId", Entity.class);
        final Column<?> col3 = forMethod("getState", Entity.class);
        new EqualsTester()
                .addEqualityGroup(col1, col2)
                .addEqualityGroup(col3)
                .testEquals();
    }

    @Test
    public void memoize_value_at_at_point_in_time() {
        final Column<Integer> mutableColumn = forMethod("getMutableState", TestEntity.class);
        final TestEntity entity = new TestEntity("");
        final int initialState = 1;
        final int changedState = 42;
        entity.setMutableState(initialState);
        final Column.MemoizedValue<Integer> memoizedState = mutableColumn.memoizeFor(entity);
        entity.setMutableState(changedState);
        final int extractedState = mutableColumn.getFor(entity);

        assertEquals(initialState, memoizedState.getValue()
                                                .intValue());
        assertEquals(changedState, extractedState);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_get_value_from_private_method() {
        final Column<Long> column = forMethod("getFortyTwoLong", TestEntity.class);
        column.getFor(new TestEntity(""));
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_memoize_value_from_private_method() {
        final Column<Long> column = forMethod("getFortyTwoLong", TestEntity.class);
        column.memoizeFor(new TestEntity(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_get_value_from_wrong_object() {
        final Column<Long> column = forMethod("getMutableState", TestEntity.class);
        column.getFor(new DifferentTestEntity(""));
    }

    @Test(expected = NullPointerException.class)
    public void check_value_if_getter_is_not_null() {
        final Column<?> column = forMethod("getNotNull", TestEntity.class);
        column.getFor(new TestEntity(""));
    }

    @Test
    public void allow_nulls_if_getter_is_nullable() {
        final Column<?> column = forMethod("getNull", TestEntity.class);
        final Object value = column.getFor(new TestEntity(""));
        assertNull(value);
    }

    @Test
    public void tell_if_property_is_nullable() {
        final Column<?> notNullColumn = forMethod("getNotNull", TestEntity.class);
        final Column<?> nullableColumn = forMethod("getNull", TestEntity.class);

        assertFalse(notNullColumn.isNullable());
        assertTrue(nullableColumn.isNullable());
    }

    @Test
    public void contain_property_type() {
        final Column<Long> column = forMethod("getFortyTwoLong", TestEntity.class);
        assertEquals(Long.TYPE, column.getType());
    }

    @Test
    public void memoize_value_regarding_nulls() {
        final Column<?> nullableColumn = forMethod("getNull", TestEntity.class);
        final Column.MemoizedValue<?> memoizedNull = nullableColumn.memoizeFor(new TestEntity(""));
        assertTrue(memoizedNull.isNull());
        assertNull(memoizedNull.getValue());
    }

    @Test
    public void memoize_value_which_has_reference_on_Column_itself() {
        final Column<Long> column = forMethod("getMutableState", TestEntity.class);
        final TestEntity entity = new TestEntity("");
        final Column.MemoizedValue<Long> memoizedValue = column.memoizeFor(entity);
        assertSame(column, memoizedValue.getSourceColumn());
    }

    private static <T> Column<T> forMethod(String name, Class<?> enclosingClass) {
        try {
            final Method result = enclosingClass.getDeclaredMethod(name);
            return Column.from(result);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestEntity extends AbstractVersionableEntity<String, Any> {

        private int mutableState = 0;

        protected TestEntity(String id) {
            super(id);
        }

        public int getMutableState() {
            return mutableState;
        }

        public void setMutableState(int mutableState) {
            this.mutableState = mutableState;
        }

        private long getFortyTwoLong() {
            return 42L;
        }

        public Object getNotNull() {
            return null;
        }

        @Nullable
        public Object getNull() {
            return null;
        }
    }

    private static class DifferentTestEntity extends AbstractVersionableEntity<String, Any> {
        protected DifferentTestEntity(String id) {
            super(id);
        }
    }
}
