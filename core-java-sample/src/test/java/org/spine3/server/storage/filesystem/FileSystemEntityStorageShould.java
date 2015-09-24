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

package org.spine3.server.storage.filesystem;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.EntityStorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.server.storage.filesystem.Helper.cleanTestData;
import static org.spine3.server.storage.filesystem.Helper.configure;

/**
 * File system implementation of {@link org.spine3.server.storage.EntityStorage} tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ClassWithTooManyMethods",
        "DuplicateStringLiteralInspection", "ConstantConditions"})
public class FileSystemEntityStorageShould {

    private static final EntityStorage<String, Any> STORAGE = FileSystemEntityStorage.newInstance();

    private Any message;


    @Before
    public void setUpTest() {
        configure(FileSystemEntityStorageShould.class);
        cleanTestData();
        message = createMessage("testTypeUrl");
    }

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {

        final Any message = STORAGE.read("nothing");
        assertNull(message);
    }

    @Test
    public void return_null_if_read_one_record_by_null_id() {

        final Any message = STORAGE.read(null);
        assertNull(message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        STORAGE.write(null, message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {
        STORAGE.write("nothing", null);
    }

    @Test
    public void save_and_read_message() {

        final String id = "testId";

        STORAGE.write(id, message);

        final Any messageFromStorage = STORAGE.read(id);

        assertEquals(message, messageFromStorage);
    }

    @Test
    public void save_and_read_several_messages() {

        final Any message1 = createMessage("typeUri_1");
        final Any message2 = createMessage("typeUri_2");
        final Any message3 = createMessage("typeUri_3");

        STORAGE.write(message1.getTypeUrl(), message1);
        STORAGE.write(message2.getTypeUrl(), message2);
        STORAGE.write(message3.getTypeUrl(), message3);

        assertEquals(message1, STORAGE.read(message1.getTypeUrl()));
        assertEquals(message2, STORAGE.read(message2.getTypeUrl()));
        assertEquals(message3, STORAGE.read(message3.getTypeUrl()));
    }

    private static Any createMessage(String typeUrl) {
        return Any.newBuilder().setTypeUrl(typeUrl).build();
    }
}
