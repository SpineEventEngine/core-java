/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.server.Entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"InstanceMethodNamingConvention", "AbstractClassWithoutAbstractMethods",
        "ConstructorNotProtectedInAbstractClass", "DuplicateStringLiteralInspection", "ConstantConditions"})
public abstract class EntityStorageShould {

    private final EntityStorage<String, StringValue> storage;

    public EntityStorageShould(EntityStorage<String, StringValue> storage) {
        this.storage = storage;
    }

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {
        final Message message = storage.read("nothing");
        assertNull(message);
    }

    @Test
    public void return_null_if_read_one_record_by_null_id() {
        final Message message = storage.read(null);
        assertNull(message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        storage.write(null, newValue("testValue"));
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {
        storage.write("nothing", null);
    }

    @Test
    public void save_and_read_message() {
        testSaveAndReadMessage("testId", "testValue");
    }

    @Test
    public void save_and_read_several_messages_with_different_ids() {
        testSaveAndReadMessage("id-1", "value-1");
        testSaveAndReadMessage("id-2", "value-2");
        testSaveAndReadMessage("id-3", "value-3");
    }

    @Test
    public void rewrite_message_if_write_with_same_id() {
        final String id = "test-id-rewrite";
        testSaveAndReadMessage(id, "value-1");
        testSaveAndReadMessage(id, "value-2");
    }

    private void testSaveAndReadMessage(String id, String value) {
        final StringValue expected = newValue(value);

        storage.write(id, expected);
        final StringValue actual = storage.read(id);

        assertEquals(expected, actual);
    }

    private static StringValue newValue(String value) {
        return StringValue.newBuilder().setValue(value).build();
    }

    public static class TestEntity extends Entity<String, StringValue> {
        protected TestEntity(String id) {
            super(id);
        }
        @Override
        protected StringValue getDefaultState() {
            return StringValue.getDefaultInstance();
        }
    }
}
