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

package io.spine.test;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Command;
import io.spine.base.Commands;
import io.spine.client.ActorRequestFactory;
import io.spine.time.Time;
import io.spine.time.ZoneOffsets;
import io.spine.users.TenantId;
import org.junit.Before;
import org.junit.Test;

import static io.spine.test.Tests.newUserUuid;
import static io.spine.test.Tests.newUuidValue;
import static io.spine.validate.Validate.checkNotDefaultState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
public class CommandTestShould {

    private CommandTest<StringValue> commandTest;

    /**
     * Creates a new command and checks its content.
     *
     * <p>If the method completes, we assume that the passed command test
     * has correctly working {@link ActorRequestFactory}.
     */
    private static void createAndAssertCommand(CommandTest<StringValue> commandTest) {
        final StringValue commandMessage = newUuidValue();
        final Command command = commandTest.createCommand(commandMessage);

        checkNotDefaultState(command);
        assertEquals(commandMessage, Commands.getMessage(command));
        checkNotDefaultState(command.getContext());
    }

    @Before
    public void setUp() {
        commandTest = new TestCommandTest();
    }

    @Test
    public void initialize_with_default_ActorRequestFactory_and_produce_commands() {
        createAndAssertCommand(commandTest);
    }

    @SuppressWarnings({"ConstantConditions" /* Passing `null` is the purpose of this test. */,
            "ResultOfObjectAllocationIgnored" /* because the constructor should fail. */})
    @Test(expected = NullPointerException.class)
    public void do_not_allow_null_ActorRequestFactory() {
        new TestCommandTest(null);
    }

    @Test
    public void accept_custom_ActorRequestFactory() {
        final Class<? extends CommandTestShould> clazz = getClass();
        final CommandTest<StringValue> commandTestWithFactory =
                new TestCommandTest(newRequestFactory(clazz));

        createAndAssertCommand(commandTestWithFactory);
    }

    /**
     * Creates a test instance of {@code ActorRequestFactory}.
     *
     * <p>The factory gets:
     * <ul>
     *     <li>generated {@code UserId} for the actor.
     *     <li>UTC zone offset
     *     <li>{@code TenantId} based on the simple name of the passed class.
     * </ul>
     */
    static ActorRequestFactory newRequestFactory(Class<?> clazz) {
        return ActorRequestFactory.newBuilder()
                             .setActor(newUserUuid())
                             .setZoneOffset(ZoneOffsets.UTC)
                             .setTenantId(TenantId.newBuilder()
                                                  .setValue(clazz.getSimpleName())
                                                  .build())
                             .build();
    }

    @Test
    public void have_empty_state_before_command_creation() {
        assertFalse(commandTest.commandMessage().isPresent());
        assertFalse(commandTest.commandContext().isPresent());
        assertFalse(commandTest.command().isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // This test verifies that Optionals
                                                     // are initialized.
    @Test
    public void stores_command_after_creation() {
        final StringValue commandMessage = newUuidValue();
        final Command command = commandTest.createCommand(commandMessage);

        assertEquals(commandMessage, commandTest.commandMessage().get());
        assertEquals(command.getContext(), commandTest.commandContext().get());
        assertEquals(command, commandTest.command().get());
    }

    @Test
    public void create_command_with_custom_Timestamp() {
        final StringValue commandMessage = newUuidValue();
        final Timestamp timestamp = TimeTests.Past.minutesAgo(5);
        final Command command = commandTest.createCommand(commandMessage, timestamp);

        assertEquals(timestamp, command.getContext()
                                       .getActorContext()
                                       .getTimestamp());
    }

    @SuppressWarnings("ConstantConditions") // Passing `null` is the purpose of the test.
    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_command_message_for_different_command() {
        commandTest.createDifferentCommand(null);
    }

    @Test
    public void create_different_command() {
        final Message anotherCommandMsg = Time.getCurrentTime();
        final Command anotherCommand = commandTest.createDifferentCommand(anotherCommandMsg);

        assertEquals(anotherCommandMsg, Commands.getMessage(anotherCommand));
    }

    @Test
    public void create_different_command_with_timestamp() {
        final Message anotherCommandMsg = Time.getCurrentTime();
        final Timestamp timestamp = TimeTests.Past.minutesAgo(30);
        final Command anotherCommand =
                commandTest.createDifferentCommand(anotherCommandMsg, timestamp);

        assertEquals(anotherCommandMsg, Commands.getMessage(anotherCommand));
        assertEquals(timestamp, anotherCommand.getContext()
                                              .getActorContext()
                                              .getTimestamp());
    }

    /**
     * The test class for verifying the behaviour of the abstract parent.
     */
    private static class TestCommandTest extends CommandTest<StringValue> {
        protected TestCommandTest(ActorRequestFactory requestFactory) {
            super(requestFactory);
        }

        protected TestCommandTest() {
            super();
        }

        @Override
        protected void setUp() {
            // We don't have an object under test for this test harness class.
        }
    }
}
