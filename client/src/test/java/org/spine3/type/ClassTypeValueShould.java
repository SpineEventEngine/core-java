/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.UInt32Value;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClassTypeValueShould {

    @Test
    public void return_enclosed_value() {
        final Class<? extends Message> expected = StringValue.class;
        assertEquals(expected, new  MessageClass(expected) {}.value());
    }

    @Test
    public void be_not_equal_to_null() {
        //noinspection ObjectEqualsNull
        assertFalse(new MessageClass(UInt32Value.class){}.equals(null));
    }

    @Test
    public void be_not_equal_to_object_of_another_class() {
        // Notice that we're creating new inner classes here with the same value passed.
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(new MessageClass(StringValue.class){}.equals(new MessageClass(StringValue.class){}));
    }

    @Test
    public void be_equal_to_self() {
        final MessageClass test = new MessageClass(UInt32Value.class){};
        //noinspection EqualsWithItself
        assertTrue(test.equals(test));
    }
}
