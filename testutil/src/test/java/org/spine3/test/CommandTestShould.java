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

package org.spine3.test;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.Commands;
import org.spine3.client.CommandFactory;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.TenantId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.test.Tests.newUserUuid;
import static org.spine3.test.Tests.newUuidValue;
import static org.spine3.validate.Validate.checkNotDefault;

/**
 * @author Alexander Yevsyukov
 */
public class CommandTestShould {

    private CommandTest<StringValue> commandTest;

    /**
     * Creates a new command and checks its content.
     *
     * <p>If the method completes, we assume that the passed command test
     * has correctly working {@link CommandFactory}.
     */
    private static void createAndAssertCommand(CommandTest<StringValue> commandTest) {
        final StringValue commandMessage = newUuidValue();
        final Command command = commandTest.createCommand(commandMessage);

        checkNotDefault(command);
        assertEquals(commandMessage, Commands.getMessage(command));
        checkNotDefault(command.getContext());
    }

    @Before
    public void setUp() {
        commandTest = new TestCommandTest();
    }

    @Test
    public void initialize_with_default_CommandFactory_and_produce_commands() {
        createAndAssertCommand(commandTest);
    }

    @SuppressWarnings({"ConstantConditions" /* Passing `null` is the purpose of this test. */,
            "ResultOfObjectAllocationIgnored" /* because the constructor should fail. */})
    @Test(expected = NullPointerException.class)
    public void do_not_allow_null_CommandFacotry() {
        new TestCommandTest(null);
    }

    @Test
    public void accept_custom_CommandFactory() {
        final CommandTest<StringValue> commandTestWithFactory = new TestCommandTest(
                CommandFactory.newBuilder()
                              .setActor(newUserUuid())
                              .setZoneOffset(ZoneOffsets.UTC)
                              .setTenantId(TenantId.newBuilder()
                                                   .setValue(getClass().getSimpleName())
                                                   .build())
                              .build()
        );

        createAndAssertCommand(commandTestWithFactory);
    }

    @Test
    public void have_empty_state_before_command_creation() {
        assertFalse(commandTest.commandMessage()
                               .isPresent());
        assertFalse(commandTest.commandContext()
                               .isPresent());
        assertFalse(commandTest.command()
                               .isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // This test verifies that Optionals are initialized.
    @Test
    public void stores_command_after_creation() {
        final StringValue commandMessage = newUuidValue();
        final Command command = commandTest.createCommand(commandMessage);

        assertEquals(commandMessage, commandTest.commandMessage()
                                                .get());
        assertEquals(command.getContext(), commandTest.commandContext()
                                                      .get());
        assertEquals(command, commandTest.command()
                                         .get());
    }

    @Test
    public void create_command_with_custom_Timestamp() {
        final StringValue commandMessage = newUuidValue();
        final Timestamp timestamp = Timestamps.minutesAgo(5);
        final Command command = commandTest.createCommand(commandMessage, timestamp);

        assertEquals(timestamp, command.getContext()
                                       .getTimestamp());
    }

    @SuppressWarnings("ConstantConditions") // Passing `null` is the purpose of the test.
    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_command_on_createOtherCommand() {
        commandTest.createAnotherCommand(null);
    }

    @Test
    public void create_another_command() {
        final Message anotherCommandMsg = Timestamps.getCurrentTime();
        final Command anotherCommand = commandTest.createAnotherCommand(anotherCommandMsg);

        assertEquals(anotherCommandMsg, Commands.getMessage(anotherCommand));
    }

    @Test
    public void create_another_command_with_timestamp() {
        final Message anotherCommandMsg = Timestamps.getCurrentTime();
        final Timestamp timestamp = Timestamps.minutesAgo(30);
        final Command anotherCommand = commandTest.createAnotherCommand(anotherCommandMsg, timestamp);

        assertEquals(anotherCommandMsg, Commands.getMessage(anotherCommand));
        assertEquals(timestamp, anotherCommand.getContext().getTimestamp());
    }

    /**
     * The test class for verifying the behaviour of the abstract parent.
     */
    private static class TestCommandTest extends CommandTest<StringValue> {
        protected TestCommandTest(CommandFactory commandFactory) {
            super(commandFactory);
        }

        protected TestCommandTest() {
            super();
        }
    }
}
