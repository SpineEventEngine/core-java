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

package io.spine.server.command.given;

import com.google.protobuf.StringValue;
import io.spine.client.ActorRequestFactory;
import io.spine.server.command.CommandTest;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class CommandTestTestEnv {

    /** Prevents instantiation of this utility class. */
    private CommandTestTestEnv() {
    }

    /**
     * The test class for verifying the behaviour of the abstract parent.
     */
    public static class TestCommandTest extends CommandTest<StringValue> {

        public TestCommandTest(ActorRequestFactory requestFactory) {
            super(requestFactory);
        }

        public TestCommandTest() {
            super();
        }

        @Override
        protected void setUp() {
            // We don't have an object under test for this test harness class.
        }
    }
}
