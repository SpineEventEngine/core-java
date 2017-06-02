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
package io.spine.protobuf;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import io.spine.time.Time;
import io.spine.users.UserId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Wrappers.pack;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;

public class MessagesShould {

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(Messages.class);
    }

    @Test
    public void return_the_same_any_from_toAny() {
        final Any any = pack(getClass().getSimpleName());
        assertSame(any, Messages.toAny(any));
    }

    @Test
    public void pack_to_Any() {
        final Timestamp timestamp = Time.getCurrentTime();
        assertEquals(timestamp, unpack(Messages.toAny(timestamp)));
    }

    @Test
    public void return_builder_for_the_message() {
        final Message.Builder messageBuilder = Messages.builderFor(UserId.class);
        assertNotNull(messageBuilder);
        assertEquals(UserId.class, messageBuilder.build().getClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_try_to_get_builder_for_not_the_generated_message() {
        Messages.builderFor(Message.class);
    }

    @Test
    public void return_true_when_message_is_checked(){
        assertTrue(Messages.isMessage(UserId.class));
    }

    @Test
    public void return_false_when_not_message_is_checked(){
        assertFalse(Messages.isMessage(getClass()));
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(Messages.class, NullPointerTester.Visibility.PACKAGE);
    }
}
