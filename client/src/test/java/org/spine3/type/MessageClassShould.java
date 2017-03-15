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

package org.spine3.type;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class MessageClassShould {

    private static final Class<StringValue> MSG_CLASS = StringValue.class;

    private TestMessageClass testMsgClass;

    @Before
    public void setUp() {
        testMsgClass = new TestMessageClass(MSG_CLASS);
    }

    @Test
    public void return_enclosed_value() {
        assertEquals(MSG_CLASS, testMsgClass.value());
    }

    @Test
    public void return_java_class_name() {
        assertEquals(ClassName.of(MSG_CLASS), testMsgClass.getClassName());
    }

    @Test
    public void convert_value_to_string() {
        assertEquals(String.valueOf(MSG_CLASS), testMsgClass.toString());
    }

    @Test
    public void return_hash_code() {
        assertEquals(Objects.hash(MSG_CLASS), testMsgClass.hashCode());
    }

    @Test
    public void be_not_equal_to_null() {
        assertNotNull(testMsgClass);
    }

    @Test
    public void be_not_equal_to_object_of_another_class() {
        // Notice that we're creating a new object with the same value passed.
        assertNotEquals(testMsgClass, new MessageClass(MSG_CLASS) {});
    }

    @Test
    public void be_equal_to_object_with_the_same_value() {
        assertEquals(testMsgClass, new TestMessageClass(MSG_CLASS));
    }

    @Test
    public void be_equal_to_self() {
        assertEquals(testMsgClass, testMsgClass);
    }

    private static class TestMessageClass extends MessageClass {

        private TestMessageClass(Class<? extends Message> value) {
            super(value);
        }
    }
}
