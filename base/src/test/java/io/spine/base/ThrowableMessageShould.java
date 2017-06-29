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
package io.spine.base;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.Timestamps;
import io.spine.protobuf.Wrapper;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class ThrowableMessageShould {

    @Test
    public void create_instance() {
        final StringValue message = Wrapper.forString(newUuid());

        final ThrowableMessage throwableMessage = new TestThrowableMessage(message);

        assertEquals(message, throwableMessage.getMessageThrown());
        assertTrue(Timestamps.isValid(throwableMessage.getTimestamp()));
    }

    /**
     * Sample {@code ThrowableMessage} class used for test purposes only.
     */
    private static class TestThrowableMessage extends ThrowableMessage {

        private static final long serialVersionUID = 0L;

        private TestThrowableMessage(GeneratedMessageV3 failure) {
            super(failure);
        }
    }
}
