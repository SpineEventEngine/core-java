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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.Timestamps;
import io.spine.base.ThrowableMessage;
import io.spine.protobuf.AnyPacker;
import io.spine.test.TestValues;
import org.junit.Test;

import static io.spine.core.Rejections.toRejection;
import static io.spine.test.TestValues.newUuidValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class RejectionsShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Rejections.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(CommandId.class, CommandId.getDefaultInstance())
                .setDefault(ThrowableMessage.class, new TestThrowableMessage(newUuidValue()))
                .testAllPublicStaticMethods(Rejections.class);
    }

    @Test
    public void generate_rejection_id_upon_command_id() {
        final CommandId commandId = Commands.generateId();
        final RejectionId actual = Rejections.generateId(commandId);

        final String expected = String.format(Rejections.REJECTION_ID_FORMAT, commandId.getUuid());
        assertEquals(expected, actual.getValue());
    }


    @Test
    public void convert_throwable_message_to_rejection_message() {
        final StringValue rejectionState = TestValues.newUuidValue();
        final CommandContext context = CommandContext.newBuilder()
                                                   .build();
        final Command command = Command.newBuilder()
                                     .setMessage(AnyPacker.pack(newUuidValue()))
                                     .setContext(context)
                                     .build();

        final TestThrowableMessage throwableMessage = new TestThrowableMessage(rejectionState);
        final Rejection rejectionWrapper = toRejection(throwableMessage, command);

        assertEquals(rejectionState, AnyPacker.unpack(rejectionWrapper.getMessage()));
        assertFalse(rejectionWrapper.getContext()
                                    .getStacktrace()
                                    .isEmpty());
        assertTrue(Timestamps.isValid(rejectionWrapper.getContext()
                                                      .getTimestamp()));
        final Command wrappedCommand = rejectionWrapper.getContext()
                                                       .getCommand();
        assertEquals(command, wrappedCommand);
    }

    /**
     * Sample {@code ThrowableMessage} class used for test purposes only.
     */
    private static class TestThrowableMessage extends ThrowableMessage {

        private static final long serialVersionUID = 0L;

        private TestThrowableMessage(GeneratedMessageV3 message) {
            super(message);
        }
    }
}
