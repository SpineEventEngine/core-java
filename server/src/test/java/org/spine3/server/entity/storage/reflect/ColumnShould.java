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
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.server.entity.AbstractVersionableEntity;
import org.spine3.server.entity.Entity;
import org.spine3.test.Given;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
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
        final Column.MemoizedValue<Integer> memiozedState = mutableColumn.memoizeFor(entity);
        entity.setMutableState(changedState);
        final int extractedState = mutableColumn.getFor(entity);

        assertEquals(initialState, memiozedState.getValue()
                                                .intValue());
        assertEquals(changedState, extractedState);
    }

    @Ignore // TODO:2017-03-28:dmytro.dashenkov: Check out this equals issue.
    @Test
    public void have_memoized_values_comparable() {
        final String stringId = "some-random-sequence";
        final Entity<String, ?> firstEntity = new TestEntity(stringId);
        final Entity<String, ?> secondEntity = new TestEntity(stringId);
        final Entity<String, ?> differentEntity = new TestEntity("different-id");
        final Column.MemoizedValue<String> firstIdMemoized = memoized("getId",
                                                                      Entity.class,
                                                                      firstEntity);
        final Column.MemoizedValue<String> secondIdMemoized = memoized("getId",
                                                                      Entity.class,
                                                                      secondEntity);
        final Column.MemoizedValue<String> differentIdMemoized = memoized("getId",
                                                                      Entity.class,
                                                                      differentEntity);
        new EqualsTester()
                .addEqualityGroup(firstIdMemoized, secondIdMemoized)
                .addEqualityGroup(differentIdMemoized)
                .testEquals();
    }

    private static <T> Column<T> forMethod(String name, Class<?> enclosingClass) {
        try {
            final Method result = enclosingClass.getDeclaredMethod(name);
            return Column.from(result);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    private static <T, E> Column.MemoizedValue<T> memoized(String name,
                                                           Class<E> enclosingClass,
                                                           E dataSource) {
        final Column<T> column = forMethod(name, enclosingClass);
        final Column.MemoizedValue<T> value =  column.memoizeFor(dataSource);
        return value;
    }

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
    }
}
