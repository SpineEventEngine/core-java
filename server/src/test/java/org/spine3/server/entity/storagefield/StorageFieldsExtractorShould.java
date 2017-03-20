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

package org.spine3.server.entity.storagefield;

import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.AbstractEntity;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.StorageFields;
import org.spine3.test.entity.Project;
import org.spine3.testdata.Sample;

import javax.annotation.Nullable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov.
 */
public class StorageFieldsExtractorShould {

    private static final String STRING_ID = Identifiers.newUuid();

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(StorageFieldsExtractor.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester()
                .testStaticMethods(StorageFieldsExtractor.class,
                                   NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void extract_no_fields_if_none_defined() {
        final Entity entity = new EntityWithNoStorageFields(STRING_ID);
        final Optional<StorageFields> fieldsOptional = StorageFieldsExtractor.extract(entity);
        assertTrue(fieldsOptional.isPresent());
        final StorageFields fields = fieldsOptional.get();

        checkId(entity, fields);
        assertEmpty(fields.getAnyFieldMap());
        assertEmpty(fields.getIntegerFieldMap());
        assertEmpty(fields.getLongFieldMap());
        assertEmpty(fields.getStringFieldMap());
        assertEmpty(fields.getBooleanFieldMap());
        assertEmpty(fields.getFloatFieldMap());
        assertEmpty(fields.getDoubleFieldMap());
    }

    @Test
    public void put_non_null_fields_to_fields_maps() {
        final EntityWithManyGetters entity = spy(new EntityWithManyGetters(STRING_ID));
        final Optional<StorageFields> fieldsOptional = StorageFieldsExtractor.extract(entity);
        assertTrue(fieldsOptional.isPresent());
        final StorageFields fields = fieldsOptional.get();

        verify(entity).getId();
        verify(entity).getFloatNull();
        verify(entity).getIntegerFieldValue();
        verify(entity).getSomeMessage();

        verify(entity, never()).getSomeNonPublicMethod();
        verify(entity, never()).getSomeUnsupportedType();
        verify(entity, never()).getSomeVoid();

        checkId(entity, fields);

        // Message fields map contains the only custom field
        final Map<String, Any> anyFields = fields.getAnyFieldMap();
        assertSize(1, anyFields);
        final Any messageField = anyFields.get("somemessage");
        assertNotNull(messageField);
        assertEquals(entity.getSomeMessage(), AnyPacker.unpack(messageField));

        // Float field map contains no entries (as field in null)
        final Map<String, Float> floatFields = fields.getFloatFieldMap();
        assertEmpty(floatFields);

        // Integer field map containt the only entry
        final Map<String, Integer> intFields = fields.getIntegerFieldMap();
        assertSize(1, intFields);
        final Integer field = intFields.get("IntegerFieldValue".toLowerCase());
        assertNotNull(field);
        assertEquals(entity.getIntegerFieldValue(), field.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void throw_if_non_null_method_returns_null() {
        StorageFieldsExtractor.extract(new EntityWithInvalidGetters(STRING_ID));
    }

    @Test
    public void hadnle_non_public_entity_class() {
        final Optional<StorageFields> fields =
                StorageFieldsExtractor.extract(new PrivateEntity(STRING_ID));
        assertFalse(fields.isPresent());
    }

    @Test
    public void ignore_static_members() {
        final Optional<StorageFields> fieldsOptional =
                StorageFieldsExtractor.extract(new EntityWithManyGetters(STRING_ID));
        assertTrue(fieldsOptional.isPresent());
        final StorageFields fields = fieldsOptional.get();
        final Map<String, Integer> ints = fields.getIntegerFieldMap();
        final Integer staticValue = ints.get("StaticMember".toLowerCase());
        assertNull(staticValue);
    }

    private static void checkId(Entity entity, StorageFields fields) {
        @SuppressWarnings("OverlyStrongTypeCast") // ...OrBuilder
        final String id = ((StringValue) AnyPacker.unpack(fields.getEntityId())).getValue();
        final String entityId = (String) entity.getId();
        assertEquals("StorageFields contain not the same entity ID as passed",
                     entityId,
                     id);
    }

    public static class EntityWithNoStorageFields extends AbstractEntity<String, Any> {
        protected EntityWithNoStorageFields(String id) {
            super(id);
        }
    }

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

        public byte[] getSomeUnsupportedType() {
            throw new AssertionError("getSomeUnsupportedType invoked");
        }

        public static int getStaticMember() {
            return 1024;
        }
    }

    public static class EntityWithInvalidGetters extends AbstractEntity<String, Any> {

        protected EntityWithInvalidGetters(String id) {
            super(id);
        }

        @SuppressWarnings("ReturnOfNull") // required for the test
        public Boolean getNonNullBooleanField() {
            return null;
        }
    }

    private static class PrivateEntity extends AbstractEntity<String, Any> {

        protected PrivateEntity(String id) {
            super(id);
        }
    }
}
