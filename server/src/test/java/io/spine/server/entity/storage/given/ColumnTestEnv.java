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

package io.spine.server.entity.storage.given;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import io.spine.core.Version;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.Enumerated;
import io.spine.validate.StringValueVBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.server.entity.storage.given.ColumnTestEnv.TaskStatus.SUCCESS;
import static io.spine.testing.Tests.nullRef;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dmytro Grankin
 */
public class ColumnTestEnv {

    public static final String CUSTOM_COLUMN_NAME = " customColumnName ";

    private ColumnTestEnv() {
        // Prevent instantiation of this utility class.
    }

    public static EntityColumn forMethod(String name, Class<?> enclosingClass) {
        try {
            Method result = enclosingClass.getDeclaredMethod(name);
            return EntityColumn.from(result);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestEntity
            extends AbstractVersionableEntity<String, StringValue> {

        private int mutableState = 0;

        public TestEntity(String id) {
            super(id);
        }

        @Column
        public int getMutableState() {
            return mutableState;
        }

        public void setMutableState(int mutableState) {
            this.mutableState = mutableState;
        }

        @Column
        public String getNotNull() {
            return nullRef();
        }

        @Column
        public @Nullable String getNull() {
            return null;
        }

        @Column
        public long getLong() {
            return 0;
        }

        @Column
        public TaskStatus getEnumNotAnnotated() {
            return SUCCESS;
        }

        @Column
        @Enumerated
        public TaskStatus getEnumOrdinal() {
            return SUCCESS;
        }

        @Column
        @Enumerated(STRING)
        public TaskStatus getEnumString() {
            return SUCCESS;
        }

        @Column
        private long getFortyTwoLong() {
            return 42L;
        }

        @Column
        public String getParameter(String param) {
            return param;
        }

        @Column
        public static String getStatic() {
            return "";
        }
    }

    public static class TestAggregate extends Aggregate<Long, StringValue, StringValueVBuilder> {

        protected TestAggregate(Long id) {
            super(id);
        }
    }

    public static class BrokenTestEntity extends AbstractEntity<String, Any> {
        protected BrokenTestEntity(String id) {
            super(id);
        }

        // non-serializable Entity EntityColumn
        @Column
        public Object getFoo() {
            fail("Invoked a getter for a non-serializable EntityColumn BrokenTestEntity.foo");
            return new Object();
        }
    }

    public static class EntityRedefiningColumnAnnotation
            extends AbstractEntity<String, Any> implements VersionableEntity<String, Any> {
        protected EntityRedefiningColumnAnnotation(String id) {
            super(id);
        }

        @Column
        @Override
        public Version getVersion() {
            return Version.getDefaultInstance();
        }
    }

    public static class EntityWithDefaultColumnNameForStoring
            extends AbstractVersionableEntity<String, Any> {
        protected EntityWithDefaultColumnNameForStoring(String id) {
            super(id);
        }

        @Column
        public int getValue() {
            return 0;
        }
    }

    public static class EntityWithCustomColumnNameForStoring
            extends AbstractVersionableEntity<String, Any> {
        public EntityWithCustomColumnNameForStoring(String id) {
            super(id);
        }

        @Column(name = CUSTOM_COLUMN_NAME)
        public int getValue() {
            return 0;
        }
    }

    public enum TaskStatus {
        QUEUED,
        EXECUTING,
        FAILED,
        SUCCESS
    }
}
