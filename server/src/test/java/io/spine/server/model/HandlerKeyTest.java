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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Grankin
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("HandlerKey should")
class HandlerKeyTest {

    private final CommandClass emptyClass = CommandClass.from(Empty.class);
    private final CommandClass stringClass = CommandClass.from(StringValue.class);

    @Test
    @DisplayName("not accept nulls on construction")
    void notAcceptNulls() {
        new NullPointerTester()
                .setDefault(MessageClass.class, emptyClass)
                .testAllPublicStaticMethods(HandlerKey.class);
    }

    @Test
    @DisplayName("return command class of Empty if there is no origin")
    void getEmptyClassWhenNoOrigin() {
        CommandClass handledMessage = stringClass;
        HandlerKey key = HandlerKey.of(handledMessage);
        assertEquals(handledMessage, key.getHandledMessageCls());
        assertEquals(emptyClass, key.getOriginCls());
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        HandlerKey first = HandlerKey.of(stringClass);
        HandlerKey second = HandlerKey.of(stringClass, stringClass);
        HandlerKey third = HandlerKey.of(emptyClass, stringClass);

        new EqualsTester().addEqualityGroup(first)
                          .addEqualityGroup(second)
                          .addEqualityGroup(third)
                          .testEquals();
    }
}
