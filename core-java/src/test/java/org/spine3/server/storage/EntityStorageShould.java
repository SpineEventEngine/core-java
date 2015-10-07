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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.test.TestIdWithStringField;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection", "ConstantConditions",
        "AbstractClassWithoutAbstractMethods", "ConstructorNotProtectedInAbstractClass"})
public abstract class EntityStorageShould {

    private final EntityStorage<String, TestIdWithStringField> storage;

    public EntityStorageShould(EntityStorage<String, TestIdWithStringField> storage) {
        this.storage = storage;
    }


    @Test
    public void return_empty_message_if_read_one_record_from_empty_storage() {

        final Message message = storage.read("nothing");
        assertEquals(Any.getDefaultInstance(), message);
    }

    @Test
    public void return_empty_message_if_read_one_record_by_null_id() {

        final Message message = storage.read(null);
        assertEquals(Any.getDefaultInstance(), message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {

        storage.write(null, createMessage("testId"));
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {

        storage.write("nothing", null);
    }

    @Test
    public void save_and_read_message() {
        testSaveAndReadMessage("testId");
    }

    @Test
    public void save_and_read_several_messages() {

        testSaveAndReadMessage("testId_1");
        testSaveAndReadMessage("testId_2");
        testSaveAndReadMessage("testId_3");
    }

    private void testSaveAndReadMessage(String messageId) {

        final TestIdWithStringField expected = createMessage(messageId);

        storage.write(expected.getId(), expected);
        waitIfNeeded(4);

        final TestIdWithStringField actual = storage.read(expected.getId());

        assertEquals(expected, actual);
    }

    private static TestIdWithStringField createMessage(String id) {
        return TestIdWithStringField.newBuilder().setId(id).build();
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    protected void waitIfNeeded(long seconds) {
        // NOP
    }
}
