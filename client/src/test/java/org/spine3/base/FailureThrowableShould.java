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

package org.spine3.base;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.Timestamps;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps2;
import org.spine3.test.TestCommandFactory;
import org.spine3.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;

public class FailureThrowableShould {

    private Command command;

    @Before
    public void setUp() {
        final TestCommandFactory commandFactory =
                TestCommandFactory.newInstance(newUuid(),
                                               ZoneOffset.getDefaultInstance());
        this.command = commandFactory.create(newStringValue(newUuid()),
                                             Timestamps2.getCurrentTime());
    }

    @Test
    public void create_instance() {
        final StringValue failure = newStringValue(newUuid());

        final FailureThrowable failureThrowable = new TestFailure(command.getMessage(),
                                                                  command.getContext(),
                                                                  failure);

        assertEquals(failure, failureThrowable.getFailureMessage());
        assertTrue(Timestamps.isValid(failureThrowable.getTimestamp()));
    }

    @Test
    public void convert_to_failure_message() {
        final StringValue failure = newStringValue(newUuid());

        final Failure failureWrapper = new TestFailure(command.getMessage(),
                                                       command.getContext(),
                                                       failure).toFailure();

        assertEquals(failure, AnyPacker.unpack(failureWrapper.getMessage()));
        assertFalse(failureWrapper.getContext()
                                  .getStacktrace()
                                  .isEmpty());
        assertTrue(Timestamps.isValid(failureWrapper.getContext()
                                                    .getTimestamp()));
    }

    private static class TestFailure extends FailureThrowable {

        protected TestFailure(GeneratedMessageV3 commandMessage,
                              CommandContext context,
                              GeneratedMessageV3 failure) {
            super(commandMessage, context, failure);
        }

        private static final long serialVersionUID = 0L;
    }
}
