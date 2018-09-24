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

package io.spine.server.model.given;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.spine.base.RejectionMessage;
import io.spine.base.ThrowableMessage;

import java.io.IOException;

public class MethodExceptionCheckerTestEnv {

    /** Prevents instantiation of this utility class. */
    private MethodExceptionCheckerTestEnv() {
    }

    @SuppressWarnings("unused") // Reflective access.
    public static class StubMethodContainer {

        private static void methodNoExceptions() {
        }

        private static void methodCheckedException() throws Exception {
            throw new IOException("Test checked exception");
        }

        private static void methodRuntimeException() throws RuntimeException {
            throw new RuntimeException("Test runtime exception");
        }

        private static void methodCustomException() throws IOException {
            throw new IOException("Test custom exception");
        }

        private static void methodDescendantException() throws DescendantThrowableMessage {
            throw new DescendantThrowableMessage(Any.getDefaultInstance());
        }
    }

    private static class DescendantThrowableMessage extends ThrowableMessage {

        private static final long serialVersionUID = 0L;

        DescendantThrowableMessage(GeneratedMessageV3 message) {
            super((RejectionMessage) message);
        }
    }
}
