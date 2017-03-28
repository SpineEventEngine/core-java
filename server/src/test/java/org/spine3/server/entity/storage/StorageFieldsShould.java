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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.server.entity.AbstractEntity;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.LifecycleFlags;
import org.spine3.test.entity.Project;
import org.spine3.testdata.Sample;

import javax.annotation.Nullable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public class StorageFieldsShould {

    private static final String STRING_ID = "some-string-id-never-used";

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(StorageFields.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester()
                .testStaticMethods(StorageFields.class,
                                   NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void return_empty_map() {
        final Map<String, Column.MemoizedValue<?>> emptyFields = StorageFields.empty();
        assertNotNull(emptyFields);
        assertEmpty(emptyFields);
    }

    @Test
    public void extract_no_fields_if_none_defined() {
        final Entity entity = new EntityWithNoStorageFields(STRING_ID);
        final Map<String, Column.MemoizedValue<?>> fields = StorageFields.from(entity);
        assertNotNull(fields);
        assertEmpty(fields);
    }

    @Test
    public void put_non_null_fields_to_fields_maps() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Map<String, Column.MemoizedValue<?>> fields = StorageFields.from(entity);
        assertNotNull(fields);

        assertSize(3, fields);

        final String floatNullKey = "floatNull";
        @SuppressWarnings("unchecked") final Column.MemoizedValue<Float> floatMemoizedNull =
                (Column.MemoizedValue<Float>) fields.get(floatNullKey);
        assertNotNull(floatMemoizedNull);
        assertNull(floatMemoizedNull.getValue());

        final String intFieldKey = "integerFieldValue";
        assertEquals(entity.getIntegerFieldValue(),
                     fields.get(intFieldKey)
                           .getValue());

        final String messageKey = "someMessage";
        assertEquals(entity.getSomeMessage(),
                     fields.get(messageKey)
                           .getValue());
    }

    @Test
    public void ignore_static_members() {
        final Map<String, Column.MemoizedValue<?>> fields =
                StorageFields.from(new EntityWithManyGetters(STRING_ID));
        final Column.MemoizedValue<?> staticValue = fields.get("staticMember");
        assertNull(staticValue);
    }

    @Test
    public void handle_non_public_entity_class() {
        final Map<?, ?> fields = StorageFields.from(new PrivateEntity(STRING_ID));
        assertNotNull(fields);
        assertEmpty(fields);
    }

    @Test
    public void handle_exclusive_methods() {
        final Map<?, ?> fields = StorageFields.from(new ExclusiveMethodsEntity(STRING_ID));
        assertNotNull(fields);
        assertEmpty(fields);
    }

    public static class EntityWithNoStorageFields extends AbstractEntity<String, Any> {
        protected EntityWithNoStorageFields(String id) {
            super(id);
        }
    }

    @SuppressWarnings("unused")  // Reflective access
    public static class EntityWithManyGetters extends AbstractEntity<String, Any> {

        private final Message someMessage = Sample.messageOfType(Project.class);

        protected EntityWithManyGetters(String id) {
            super(id);
        }

        public int getIntegerFieldValue() {
            return 0;
        }

        @Nullable
        public Float getFloatNull() {
            return null;
        }

        public Message getSomeMessage() {
            return someMessage;
        }

        int getSomeNonPublicMethod() {
            throw new AssertionError("getSomeNonPublicMethod invoked");
        }

        public void getSomeVoid() {
            throw new AssertionError("getSomeVoid invoked");
        }

        public static int getStaticMember() {
            return 1024;
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public static class EntityWithInvalidGetters extends AbstractEntity<String, Any> {

        protected EntityWithInvalidGetters(String id) {
            super(id);
        }

        @SuppressWarnings("ReturnOfNull") // required for the test
        public Boolean getNonNullBooleanField() {
            return null;
        }
    }

    @SuppressWarnings("unused") // Reflective access
    private static class PrivateEntity extends AbstractEntity<String, Any> {

        protected PrivateEntity(String id) {
            super(id);
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public static class ExclusiveMethodsEntity extends AbstractEntity<String, Any> {

        protected ExclusiveMethodsEntity(String id) {
            super(id);
        }

        @Override
        public String getId() {
            throw new AssertionError("getId invoked");
        }

        @Override
        public Any getState() {
            throw new AssertionError("getState invoked");
        }

        @Override
        public Any getDefaultState() {
            throw new AssertionError("getDefaultState invoked");
        }

        public LifecycleFlags getLifecycleFlags() {
            throw new AssertionError("getLifecycleFlags invoked");
        }

        public Message getBuilder() {
            throw new AssertionError("getBuilder invoked");
        }
    }
}
