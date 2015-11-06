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
package org.spine3.util;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;

import static org.junit.Assert.assertEquals;
import static org.spine3.util.Users.newUserId;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class MessagesShould {

    private final UserId id = newUserId("messages_test");
    private final Any any = Any.pack(id);

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void toText_fail_on_null() {
        Messages.toText(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void toJson_fail_on_null() {
        Messages.toJson(null);
    }

    @Test
    public void convert_id_to_Any() {
        final Any test = Messages.toAny(id);
        assertEquals(any, test);
    }

    @Test
    public void convert_from_Any_to_id() {
        final UserId test = Messages.fromAny(any);
        assertEquals(id, test);
    }

    @Test
    public void convert_from_Any_to_protobuf_class() {

        final StringValue expected = StringValue.newBuilder().setValue("test_value").build();
        final Any expectedAny = Any.pack(expected);
        final Message actual = Messages.fromAny(expectedAny);
        assertEquals(expected, actual);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_null_id() {
        Messages.toAny(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_from_null_Any() {
        Messages.fromAny(null);
    }
}
