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

package io.spine.core;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.client.TestActorRequestFactory;
import io.spine.server.command.TestEventFactory;
import io.spine.test.TestValues;
import io.spine.test.Tests;
import io.spine.test.rejection.OperationRejections.CannotPerformBusinessOperation;
import io.spine.time.Time;
import org.junit.Before;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static io.spine.core.Messages.ensureMessage;
import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.Assert.assertEquals;

/**
 * Tests of {@link Messages} utility class.
 *
 * <p>This class is placed under the {@code server} module to simplify the dependencies. Tests use
 * {@link TestActorRequestFactory} which belongs to the {@code testutil-client} module, and
 * the earliest place where this dependency is available is the the {@code server} module.
 *
 * @author Alexander Yevsyukov
 */
public class MessagesShould {

    private final TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());

    private Command command;
    private StringValue commandMessage;
    private CannotPerformBusinessOperation rejectionMessage;
    private Message eventMessage;

    @Before
    public void setUp() {
        commandMessage = TestValues.newUuidValue();
        command = factory.createCommand(commandMessage);
        rejectionMessage = CannotPerformBusinessOperation.newBuilder()
                                                         .setOperationId(newUuid())
                                                         .build();
        eventMessage = Time.getCurrentTime();
    }

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(Messages.class);
    }

    @Test
    public void ensure_command_message() {
        assertEquals(commandMessage, ensureMessage(command));
    }

    @Test
    public void ensure_event_message() {
        final TestEventFactory eventFactory = TestEventFactory.newInstance(factory);
        final Event event = eventFactory.createEvent(eventMessage);

        assertEquals(eventMessage, ensureMessage(event));
    }

    @Test
    public void ensure_rejection_message() {
        final Rejection rejection = Rejections.createRejection(rejectionMessage, command);

        assertEquals(rejectionMessage, ensureMessage(rejection));
    }

    @Test
    public void unpack_messages() {
        assertEquals(commandMessage, ensureMessage(pack(commandMessage)));
        assertEquals(eventMessage, ensureMessage(pack(eventMessage)));
        assertEquals(rejectionMessage, ensureMessage(pack(rejectionMessage)));
    }
}
