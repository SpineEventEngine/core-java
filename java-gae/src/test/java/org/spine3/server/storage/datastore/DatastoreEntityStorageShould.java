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
import org.junit.*;
import org.spine3.TypeName;
import org.spine3.test.TestIdWithStringField;

import static org.junit.Assert.assertEquals;


@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ClassWithTooManyMethods",
        "DuplicateStringLiteralInspection", "ConstantConditions"})
public class DatastoreEntityStorageShould {

    /* TODO:2015.09.30:alexander.litus: start Local Datastore Server automatically and not ignore tests.
     * Reported an issue here:
     * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
     */

    private static final DatastoreEntityStorage<String, TestIdWithStringField> STORAGE =
            DatastoreEntityStorage.newInstance(LocalDatastoreManager.instance());

    private TestIdWithStringField message;

    @BeforeClass
    public static void setUpClass() {
        final TypeName typeName = TypeName.of(TestIdWithStringField.getDescriptor());
        LocalDatastoreManager.instance().setTypeName(typeName);
    }

    @Before
    public void setUpTest() {
        message = TestIdWithStringField.newBuilder().setId("testValue").build();
    }

    @After
    public void tearDownTest() {
        LocalDatastoreManager.instance().clear();
    }

    @Ignore
    @Test
    public void return_default_message_if_read_one_record_from_empty_storage() {

        final Message actual = STORAGE.read("testId");
        assertEquals(Any.getDefaultInstance(), actual);
    }

    @Ignore
    @Test
    public void return_default_message_if_read_one_record_by_null_id() {

        final Message actual = STORAGE.read(null);
        assertEquals(Any.getDefaultInstance(), actual);
    }

    @Ignore
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        STORAGE.write(null, message);
    }

    @Ignore
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {
        STORAGE.write("testId", null);
    }

    @Ignore
    @Test
    public void save_and_read_message() {

        final String id = "testId";

        STORAGE.write(id, message);

        final TestIdWithStringField actual = STORAGE.read(id);

        assertEquals(message, actual);
    }
}
