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

package io.spine.testing.server;

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Common assertions for server-side testing.
 *
 * @author Alexander Yevsyukov
 */
public class Assertions {

    /** Prevents instantiation of this utility class. */
    private Assertions() {
    }

    /**
     * Asserts that each of the {@code commandClass} is available in {@code expected}.
     */
    @SafeVarargs
    public static void assertCommandClasses(Collection<CommandClass> expected,
                                            Class<? extends CommandMessage>... commandClass) {
        assertContains(expected, CommandClass::from, commandClass);
    }

    /**
     * Asserts that each of the {@code eventClass} is available in {@code expected}.
     */
    @SafeVarargs
    public static void assertEventClasses(Collection<EventClass> expected,
                                          Class<? extends EventMessage>... eventClass) {
        assertContains(expected, EventClass::from, eventClass);
    }

    /**
     * Asserts that each of the {@code rejectionClass} is available in {@code expected}.
     */
    @SafeVarargs
    public static void assertRejectionClasses(Collection<RejectionClass> expected,
                                              Class<? extends RejectionMessage>... rejectionClass) {
        assertContains(expected, RejectionClass::of, rejectionClass);
    }

    @SafeVarargs
    private static <C extends MessageClass, M extends Message>
    void assertContains(Collection<C> expected,
                        Function<Class<M>, C> func,
                        Class<? extends M>...classes) {
        checkNotNull(expected);
        checkNotNull(classes);
        for (Class<? extends M> cls : classes) {
            assertNotNull(cls);
            @SuppressWarnings("unchecked") // OK for tests.
            Class<M> messageType = (Class<M>) cls;
            C messageClass = func.apply(messageType);
            assertTrue(expected.contains(messageClass));
        }
    }
}
