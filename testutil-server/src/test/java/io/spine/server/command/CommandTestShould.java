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

package io.spine.server.command;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Commands;
import io.spine.core.TenantId;
import io.spine.test.Tests;
import io.spine.test.TimeTests;
import io.spine.time.ZoneOffsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import static io.spine.core.given.GivenUserId.newUuid;
import static io.spine.test.TestValues.newUuidValue;
import static io.spine.validate.Validate.checkNotDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
public class CommandTestShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CommandTest<StringValue> commandTest;

    /**
     * Creates a new command and checks its content.
     *
     * <p>If the method completes, we assume that the passed command test
     * has correctly working {@link ActorRequestFactory}.
     */
    private static void createAndAssertCommand(CommandTest<StringValue> commandTest) {
        StringValue commandMessage = newUuidValue();
        Command command = commandTest.createCommand(commandMessage);

        checkNotDefault(command);
        assertEquals(commandMessage, Commands.getMessage(command));
        checkNotDefault(command.getContext());
    }

    /**
     * Creates a test instance of {@code ActorRequestFactory}.
     *
     * <p>The factory gets:
     * <ul>
     *   <li>generated {@code UserId} for the actor.
     *   <li>UTC zone offset
     *   <li>{@code TenantId} based on the simple name of the passed class.
     * </ul>
     */
    static ActorRequestFactory newRequestFactory(Class<?> clazz) {
        return ActorRequestFactory.newBuilder()
                                  .setActor(newUuid())
                                  .setZoneOffset(ZoneOffsets.UTC)
                                  .setTenantId(TenantId.newBuilder()
                                                       .setValue(clazz.getSimpleName())
                                                       .build())
                                  .build();
    }

    @Before
    public void setUp() {
        commandTest = new TestCommandTest();
    }

    @Test
    @DisplayName("initialize with default ActorRequestFactory and produce commands")
    void initializeWithDefaultActorRequestFactoryAndProduceCommands() {
        createAndAssertCommand(commandTest);
    }

    @SuppressWarnings({"ConstantConditions" /* Passing `null` is the purpose of this test. */,
            "ResultOfObjectAllocationIgnored" /* because the constructor should fail. */})
    @Test(expected = NullPointerException.class)
    @DisplayName("do not allow null ActorRequestFactory")
    void doNotAllowNullActorRequestFactory() {
        new TestCommandTest(null);
    }

    @Test
    @DisplayName("accept custom ActorRequestFactory")
    void acceptCustomActorRequestFactory() {
        Class<? extends CommandTestShould> clazz = getClass();
        CommandTest<StringValue> commandTestWithFactory =
                new TestCommandTest(newRequestFactory(clazz));

        createAndAssertCommand(commandTestWithFactory);
    }

    @Test
    @DisplayName("have empty state before command creation")
    void haveEmptyStateBeforeCommandCreation() {
        assertFalse(commandTest.commandMessage()
                               .isPresent());
        assertFalse(commandTest.commandContext()
                               .isPresent());
        assertFalse(commandTest.command()
                               .isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // This test verifies that Optionals
    // are initialized.
    @Test
    @DisplayName("stores command after creation")
    void storesCommandAfterCreation() {
        StringValue commandMessage = newUuidValue();
        Command command = commandTest.createCommand(commandMessage);

        assertEquals(commandMessage, commandTest.commandMessage()
                                                .get());
        assertEquals(command.getContext(), commandTest.commandContext()
                                                      .get());
        assertEquals(command, commandTest.command()
                                         .get());
    }

    @Test
    @DisplayName("create command with custom Timestamp")
    void createCommandWithCustomTimestamp() {
        StringValue commandMessage = newUuidValue();
        Timestamp timestamp = TimeTests.Past.minutesAgo(5);
        Command command = commandTest.createCommand(commandMessage, timestamp);

        assertEquals(timestamp, command.getContext()
                                       .getActorContext()
                                       .getTimestamp());
    }

    @Test
    @DisplayName("do not accept null command message for different command")
    void doNotAcceptNullCommandMessageForDifferentCommand() {
        thrown.expect(NullPointerException.class);
        commandTest.createDifferentCommand(Tests.nullRef());
    }

    @Test
    @DisplayName("create different command")
    void createDifferentCommand() {
        Message anotherCommandMsg = Time.getCurrentTime();
        Command anotherCommand = commandTest.createDifferentCommand(anotherCommandMsg);

        assertEquals(anotherCommandMsg, Commands.getMessage(anotherCommand));
    }

    @Test
    @DisplayName("create different command with timestamp")
    void createDifferentCommandWithTimestamp() {
        Message anotherCommandMsg = Time.getCurrentTime();
        Timestamp timestamp = TimeTests.Past.minutesAgo(30);
        Command anotherCommand =
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

        private TestCommandTest(ActorRequestFactory requestFactory) {
            super(requestFactory);
        }

        private TestCommandTest() {
            super();
        }

        @Override
        protected void setUp() {
            // We don't have an object under test for this test harness class.
        }
    }
}
