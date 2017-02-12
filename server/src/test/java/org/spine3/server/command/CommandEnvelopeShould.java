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

package org.spine3.server.command;

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.Commands;
import org.spine3.server.type.CommandClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.validate.Validate.isDefault;

/**
 * @author Alexander Yevsyukov
 */
public class CommandEnvelopeShould {

    private Command command;
    private CommandEnvelope envelope;

    @Before
    public void setUp() {
        command = createProject();
        envelope = new CommandEnvelope(command);
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
        final Message commandMessage = envelope.getCommandMessage();
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

    @SuppressWarnings({"EqualsWithItself", "EqualsBetweenInconvertibleTypes"}) // are parts of this test
    @Test
    public void have_equals() {

        assertEquals(envelope, new CommandEnvelope(command));
        assertTrue(envelope.equals(envelope));
        assertFalse(envelope.equals(command));
    }

    @Test
    public void have_hashCode() {
        assertNotEquals(System.identityHashCode(envelope), envelope.hashCode());
    }
}
