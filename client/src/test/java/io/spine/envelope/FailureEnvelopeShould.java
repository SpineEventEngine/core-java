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

import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.base.Command;
import io.spine.base.Failure;
import io.spine.base.Failures;
import io.spine.protobuf.AnyPacker;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.failures.Failures.CannotPerformBusinessOperation;
import io.spine.type.FailureClass;
import org.junit.Test;

import static io.spine.base.Identifier.newUuid;
import static org.junit.Assert.assertEquals;

/**
 * @author Alex Tymchenko
 */
public class FailureEnvelopeShould extends MessageEnvelopeShould<Failure,
                                                                 FailureEnvelope,
                                                                 FailureClass> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(FailureEnvelopeShould.class);

    @Override
    protected Failure outerObject() {
        final Message commandMessage = Int32Value.getDefaultInstance();
        final Command command = requestFactory.command().create(commandMessage);
        final Message failureMessage = CannotPerformBusinessOperation.newBuilder()
                                                            .setOperationId(newUuid())
                                                            .build();
        final Failure failure = Failures.createFailure(failureMessage, command);
        return failure;
    }

    @Override
    protected FailureEnvelope toEnvelope(Failure obj) {
        return FailureEnvelope.of(obj);
    }

    @Override
    protected FailureClass getMessageClass(Failure obj) {
        return FailureClass.of(obj);
    }

    @Test
    public void obtain_command_context() {
        final Failure failure = outerObject();
        final Command command = failure.getContext()
                                       .getCommand();
        final FailureEnvelope envelope = toEnvelope(failure);
        assertEquals(command.getContext(), envelope.getCommandContext());
    }

    @Test
    public void obtain_command_message() {
        final Failure failure = outerObject();
        final Command command = failure.getContext()
                                       .getCommand();
        final Message commandMessage = AnyPacker.unpack(command.getMessage());
        final FailureEnvelope envelope = toEnvelope(failure);
        assertEquals(commandMessage, envelope.getCommandMessage());
    }
}
