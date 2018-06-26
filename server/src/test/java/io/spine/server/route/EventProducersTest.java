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

package io.spine.server.route;

import com.google.protobuf.Message;
import io.spine.test.Tests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("EventProducers utility should")
class EventProducersTest {

    @Test
    @DisplayName("have utility ctor")
    void haveUtilityCtor() {
        Tests.assertHasPrivateParameterlessCtor(EventProducers.class);
    }

    @Test
    @DisplayName("create function for taking id fromContext")
    void createFunctionForTakingIdFromContext() {
        final EventRoute<Object, Message> fn = EventProducers.fromContext();
        assertFunction(fn);
    }

    @Test
    @DisplayName("create function for getting id fromFirstMessageField")
    void createFunctionForGettingIdFromFirstMessageField() {
        final EventRoute<Object, Message> fn = EventProducers.fromFirstMessageField();
        assertFunction(fn);
    }

    private static void assertFunction(EventRoute<Object, Message> fn) {
        assertNotNull(fn);

        // Check that custom toString() is provided.
        assertFalse(fn.toString().contains(EventRoute.class.getSimpleName()));
    }
}
