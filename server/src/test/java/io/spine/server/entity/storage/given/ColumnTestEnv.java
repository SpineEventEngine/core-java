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

package io.spine.server.entity.storage.given;

import com.google.protobuf.Any;
import io.spine.core.Version;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.entity.storage.Column;

import javax.annotation.Nullable;

import static io.spine.test.Tests.nullRef;
import static org.junit.Assert.fail;

/**
 * @author Dmytro Grankin
 */
public class ColumnTestEnv {

    private ColumnTestEnv() {
        // Prevent instantiation of this utility class.
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestEntity extends AbstractVersionableEntity<String, Any> {

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

        @Nullable
        @Column
        public String getNull() {
            return null;
        }

        @Column
        public long getLong() {
            return 0;
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
}
