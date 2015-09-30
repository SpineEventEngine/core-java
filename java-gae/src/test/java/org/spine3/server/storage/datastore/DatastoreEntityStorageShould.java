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

package org.spine3.server.storage.datastore;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.TypeName;
import org.spine3.test.TestIdWithStringField;

import static org.junit.Assert.assertEquals;


@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ClassWithTooManyMethods",
        "DuplicateStringLiteralInspection", "ConstantConditions"})
public class DatastoreEntityStorageShould {

    // TODO:2015.09.30:alexander.litus: start Local Datastore Server automatically and not ignore tests

    private DatastoreEntityStorage<String, TestIdWithStringField> storage;
    private TestIdWithStringField message;

    @Before
    public void setUpTest() {

        final TypeName typeName = TypeName.of(TestIdWithStringField.getDescriptor());
        storage = new DatastoreEntityStorage<>(typeName);
        message = TestIdWithStringField.newBuilder().setId("testValue").build();
    }

    @After
    public void tearDownTest() {
        storage.clear();
    }

    @Ignore
    @Test
    public void return_default_message_if_read_one_record_from_empty_storage() {

        final Message actual = storage.read("testId");
        assertEquals(Any.getDefaultInstance(), actual);
    }

    @Ignore
    @Test
    public void return_default_message_if_read_one_record_by_null_id() {

        final Message actual = storage.read(null);
        assertEquals(Any.getDefaultInstance(), actual);
    }

    @Ignore
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        storage.write(null, message);
    }

    @Ignore
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {
        storage.write("testId", null);
    }

    @Ignore
    @Test
    public void save_and_read_message() {

        final String id = "testId";

        storage.write(id, message);

        final TestIdWithStringField actual = storage.read(id);

        assertEquals(message, actual);
    }
}
