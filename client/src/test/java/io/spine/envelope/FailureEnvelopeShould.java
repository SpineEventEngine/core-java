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
package io.spine.envelope;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.base.Command;
import io.spine.base.Failure;
import io.spine.base.FailureClass;
import io.spine.base.Failures;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.failures.Failures.CannotPerformBusinessOperation;
import org.junit.Before;
import org.junit.Test;

import static io.spine.base.Identifier.newUuid;
import static org.junit.Assert.assertEquals;

/**
 * @author Alex Tymchenko
 */
public class FailureEnvelopeShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(FailureEnvelopeShould.class);


    private FailureEnvelope envelope;
    private CannotPerformBusinessOperation failureMessage;
    private Command command;
    private Message commandMessage;

    @Before
    public void setUp() {
        this.commandMessage = Int32Value.getDefaultInstance();
        this.command = requestFactory.command().create(commandMessage);
        this.failureMessage = CannotPerformBusinessOperation.newBuilder()
                                                            .setOperationId(newUuid())
                                                            .build();
        final Failure failure = Failures.createFailure(failureMessage, command);
        this.envelope = FailureEnvelope.of(failure);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Failure.class, Failure.getDefaultInstance())
                .testAllPublicStaticMethods(FailureEnvelope.class);
    }

    @Test
    public void obtain_failure_message() {
        assertEquals(failureMessage, envelope.getMessage());
    }

    @Test
    public void obtain_failure_message_class() {
        assertEquals(FailureClass.of(this.failureMessage), envelope.getMessageClass());
    }

    @Test
    public void obtain_command_context() {
        assertEquals(command.getContext(), envelope.getCommandContext());
    }

    @Test
    public void obtain_command_message() {
        assertEquals(commandMessage, envelope.getCommandMessage());
    }
}
