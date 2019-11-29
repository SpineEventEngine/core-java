/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server;

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Useful assertions for server-side tests.
 */
public final class ServerAssertions {

    /** Prevents instantiation of this utility class. */
    private ServerAssertions() {
    }

    /**
     * Asserts that passed event classes are exactly as expected.
     */
    @SafeVarargs
    public static void assertExactly(Set<EventClass> eventClasses,
                                     Class<? extends EventMessage>... expected) {
        assertThat(eventClasses).containsExactlyElementsIn(EventClass.setOf(expected));
    }

    /**
     * Asserts that passed command classes are exactly as expected.
     */
    @SafeVarargs
    public static void assertCommandsExactly(Set<CommandClass> commandClasses,
                                             Class<? extends CommandMessage>... expected) {
        assertThat(commandClasses).containsExactlyElementsIn(CommandClass.setOf(expected));
    }
}
