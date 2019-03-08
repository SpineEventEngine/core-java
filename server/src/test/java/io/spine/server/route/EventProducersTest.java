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

package io.spine.server.route;

import io.spine.base.EventMessage;
import io.spine.testing.Tests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("EventProducers utility should")
class EventProducersTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        Tests.assertHasPrivateParameterlessCtor(EventProducers.class);
    }

    @Test
    @DisplayName("create function for taking ID from context")
    void getIdFromContext() {
        EventRoute<?, EventMessage> fn = EventRoute.byProducerId();
        assertFunction(fn);
    }

    @Test
    @DisplayName("create function for getting ID from first message field")
    void getIdFromFirstMsgField() {
        EventRoute<?, EventMessage> fn = EventRoute.byFirstMessageField();
        assertFunction(fn);
    }

    private static void assertFunction(EventRoute<?, ?> fn) {
        assertNotNull(fn);

        // Check that custom toString() is provided.
        assertFalse(fn.toString().contains(EventRoute.class.getSimpleName()));
    }
}
