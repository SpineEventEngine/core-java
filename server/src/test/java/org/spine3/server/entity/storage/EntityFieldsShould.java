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

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.StorageFieldType;
import org.spine3.server.entity.StorageFields;
import org.spine3.test.entity.Project;
import org.spine3.testdata.Sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.spine3.server.entity.storage.EntityFields.toStorageFieldType;

/**
 * @author Dmytro Dashenkov
 */
public class EntityFieldsShould {

    @Test
    public void map_integer_to_storage_field_type() {
        checkClassToTypeMapping(int.class, StorageFieldType.INTEGER);
        checkClassToTypeMapping(Integer.class, StorageFieldType.INTEGER);
    }

    @Test
    public void map_long_to_storage_field_type() {
        checkClassToTypeMapping(long.class, StorageFieldType.LONG);
        checkClassToTypeMapping(Long.class, StorageFieldType.LONG);
    }

    @Test
    public void map_string_to_storage_field_type() {
        checkClassToTypeMapping(String.class, StorageFieldType.STRING);
    }

    @Test
    public void map_boolean_to_storage_field_type() {
        checkClassToTypeMapping(boolean.class, StorageFieldType.BOOLEAN);
        checkClassToTypeMapping(Boolean.class, StorageFieldType.BOOLEAN);
    }

    @Test
    public void map_float_to_storage_field_type() {
        checkClassToTypeMapping(float.class, StorageFieldType.FLOAT);
        checkClassToTypeMapping(Float.class, StorageFieldType.FLOAT);
    }

    @Test
    public void map_double_to_storage_field_type() {
        checkClassToTypeMapping(double.class, StorageFieldType.DOUBLE);
        checkClassToTypeMapping(Double.class, StorageFieldType.DOUBLE);
    }

    @Test
    public void map_message_to_storage_field_type() {
        checkClassToTypeMapping(Message.class, StorageFieldType.MESSAGE);
    }

    @Test
    public void map_custom_message_to_storage_field_type() {
        checkClassToTypeMapping(Any.class, StorageFieldType.MESSAGE);
        checkClassToTypeMapping(Project.class, StorageFieldType.MESSAGE);
    }

    @Test
    public void put_ints_into_storage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String intKey = "int_value";
        final int intValue = 42;

        EntityFields.putValue(builder, intKey, intValue);
        final StorageFields fields = builder.build();
        assertEquals(fields.getIntegerFieldOrDefault(intKey, -1), intValue);
    }

    @Test
    public void put_longs_into_storage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String longKey = "long_value";
        final long longValue = 42L;

        EntityFields.putValue(builder, longKey, longValue);
        final StorageFields fields = builder.build();
        assertEquals(fields.getLongFieldOrDefault(longKey, -1L), longValue);
    }

    @Test
    public void put_strings_into_storage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String stringKey = "string_value";
        final String stringValue = "some string";

        EntityFields.putValue(builder, stringKey, stringValue);
        final StorageFields fields = builder.build();
        assertEquals(fields.getStringFieldOrDefault(stringKey, ""), stringValue);
    }

    @Test
    public void put_booleans_into_storage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String boolKey = "bool_value";
        final boolean boolValue = true;

        EntityFields.putValue(builder, boolKey, boolValue);
        final StorageFields fields = builder.build();
        assertEquals(fields.getBooleanFieldOrDefault(boolKey, !boolValue), boolValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_putting_a_field_of_unknown_type() {
        EntityFields.putValue(StorageFields.newBuilder(), "mock-name", new Object());
    }

    @Test
    public void put_floats_intoStorage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String floatKey = "float_value";
        final float floatValue = 4.2f;

        EntityFields.putValue(builder, floatKey, floatValue);
        final StorageFields fields = builder.build();
        assertEquals(fields.getFloatFieldOrDefault(floatKey, 0.0f), floatValue, 0.0f);
    }

    @Test
    public void put_doubles_into_storage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String doubleKey = "double_value";
        final double doubleValue = 42.0;

        EntityFields.putValue(builder, doubleKey, doubleValue);
        final StorageFields fields = builder.build();
        assertEquals(fields.getDoubleFieldOrDefault(doubleKey, 0.0), doubleValue, 0.0);
    }

    @Test
    public void put_messages_into_storage_fields() {
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final String messageKey = "message_value";
        final Message messageValue = Sample.messageOfType(Project.class);

        EntityFields.putValue(builder, messageKey, messageValue);
        final StorageFields fields = builder.build();
        final Message messageActual =
                AnyPacker.unpack(fields.getAnyFieldOrDefault(messageKey, Any.getDefaultInstance()));
        assertEquals(messageActual, messageValue);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // after proper assertion
    private static void checkClassToTypeMapping(Class cls, StorageFieldType type) {
        final Optional<StorageFieldType> actualType = toStorageFieldType(cls);
        assertTrue(actualType.isPresent());
        assertSame(type, actualType.get());
    }
}
