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

package org.spine3.envelope;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandClass;
import org.spine3.base.Commands;
import org.spine3.protobuf.Timestamps2;
import org.spine3.test.TestCommandFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Tests.newUuidValue;
import static org.spine3.validate.Validate.isDefault;

/**
 * @author Alexander Yevsyukov
 */
public class CommandEnvelopeShould {

    private final TestCommandFactory commandFactory =
            TestCommandFactory.newInstance(CommandEnvelopeShould.class);

    private Command command;
    private CommandEnvelope envelope;

    @Before
    public void setUp() {
        command = commandFactory.createCommand(newUuidValue());
        envelope = CommandEnvelope.of(command);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .testAllPublicStaticMethods(CommandEnvelope.class);
    }

    @Test
    public void obtain_command() {
        assertEquals(command, envelope.getCommand());
    }

    @Test
    public void obtain_command_context() {
        assertEquals(command.getContext(), envelope.getCommandContext());
    }

    @Test
    public void extract_command_message() {
        final Message commandMessage = envelope.getMessage();
        assertNotNull(commandMessage);
        assertFalse(isDefault(commandMessage));
    }

    @Test
    public void obtain_command_class() {
        assertEquals(CommandClass.of(command), envelope.getCommandClass());
    }

    @Test
    public void obtain_command_id() {
        assertEquals(Commands.getId(command), envelope.getCommandId());
    }

    @Test
    public void have_equals() {
        final Command anotherCommand = commandFactory.createCommand(Timestamps2.getCurrentTime());

        new EqualsTester().addEqualityGroup(envelope)
                          .addEqualityGroup(CommandEnvelope.of(anotherCommand))
                          .testEquals();
    }

    @Test
    public void have_hashCode() {
        assertNotEquals(System.identityHashCode(envelope), envelope.hashCode());
    }
}
