/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.core.CommandClass;
import io.spine.type.MessageClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Grankin
 */
public class HandlerKeyShould {

    private final CommandClass emptyClass = CommandClass.of(Empty.class);
    private final CommandClass stringClass = CommandClass.of(StringValue.class);

    @Test
    public void not_accept_nulls_on_construction() {
        new NullPointerTester()
                .setDefault(MessageClass.class, emptyClass)
                .testAllPublicStaticMethods(HandlerKey.class);
    }

    @Test
    public void return_command_class_of_empty_if_there_is_no_origin() {
        final CommandClass handledMessage = stringClass;
        final HandlerKey key = HandlerKey.of(handledMessage);
        assertEquals(handledMessage, key.getHandledMessageCls());
        assertEquals(emptyClass, key.getOriginCls());
    }

    @Test
    public void support_equality() {
        final HandlerKey first = HandlerKey.of(stringClass);
        final HandlerKey second = HandlerKey.of(stringClass, stringClass);
        final HandlerKey third = HandlerKey.of(emptyClass, stringClass);

        new EqualsTester().addEqualityGroup(first)
                          .addEqualityGroup(second)
                          .addEqualityGroup(third)
                          .testEquals();
    }
}
