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
package io.spine.envelope;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.MessageEnvelopeTest;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.protobuf.AnyPacker;
import io.spine.test.rejection.OperationRejections.CannotPerformBusinessOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link RejectionEnvelope}.
 *
 * <p>This test suite is placed under the {@code server} to ease the inter-module dependencies.
 *
 * @author Alex Tymchenko
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Similar test cases to CommandEnvelopeTest.
@DisplayName("RejectionEnvelope should")
class RejectionEnvelopeTest extends MessageEnvelopeTest<Rejection,
                                                        RejectionEnvelope,
                                                        RejectionClass> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(RejectionEnvelopeTest.class);

    @Override
    protected Rejection outerObject() {
        Message commandMessage = Int32Value.getDefaultInstance();
        Command command = requestFactory.command()
                                              .create(commandMessage);
        Message rejectionMessage = CannotPerformBusinessOperation.newBuilder()
                                                                       .setOperationId(newUuid())
                                                                       .build();
        Rejection rejection = Rejections.createRejection(rejectionMessage, command);
        return rejection;
    }

    @Override
    protected RejectionEnvelope toEnvelope(Rejection obj) {
        return RejectionEnvelope.of(obj);
    }

    @Override
    protected RejectionClass getMessageClass(Rejection obj) {
        return RejectionClass.of(obj);
    }

    @Test
    @DisplayName("obtain command context")
    void getCommandContext() {
        Rejection rejection = outerObject();
        Command command = rejection.getContext()
                                         .getCommand();
        RejectionEnvelope envelope = toEnvelope(rejection);
        assertEquals(command.getContext(), envelope.getCommandContext());
    }

    @Test
    @DisplayName("obtain command message")
    void getCommandMessage() {
        Rejection rejection = outerObject();
        Command command = rejection.getContext()
                                         .getCommand();
        Message commandMessage = AnyPacker.unpack(command.getMessage());
        RejectionEnvelope envelope = toEnvelope(rejection);
        assertEquals(commandMessage, envelope.getCommandMessage());
    }

    @Test
    @DisplayName("obtain actor context")
    void getActorContext() {
        RejectionEnvelope rejection = toEnvelope(outerObject());
        ActorContext actorContext = rejection.getActorContext();

        /* Since we're using `TestActorRequestFactory` initialized with the class of this test suite
           the actor ID should be the suite class name.
         */
        assertEquals(getClass().getName(), actorContext.getActor()
                                                       .getValue());
    }
}
