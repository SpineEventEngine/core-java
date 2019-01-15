/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.protobuf.Durations2;
import io.spine.string.Stringifiers;
import io.spine.test.commands.CmdCreateProject;
import io.spine.test.commands.CmdStartProject;
import io.spine.test.commands.CmdStopProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.command.TestCommandMessage;
import io.spine.testing.core.given.GivenCommandContext;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.Descriptors.FileDescriptor;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Commands.sameActorAndTenant;
import static io.spine.core.Commands.wereWithinPeriod;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;
import static io.spine.time.testing.TimeTests.Past.secondsAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@linkplain Commands Commands utility class}.
 *
 * <p>The test suite is located under the "client" module since actor request generation
 * is required. So we want to avoid circular dependencies between "core" and "client" modules.
 */
@DisplayName("Commands utility should")
class CommandsTest {

    private static final FileDescriptor DEFAULT_FILE_DESCRIPTOR = Any.getDescriptor()
                                                                     .getFile();

    private static final CmdCreateProject createProject = CmdCreateProject
            .newBuilder()
            .setId(Identifier.newUuid())
            .build();
    private static final CmdStartProject startProject = CmdStartProject
            .newBuilder()
            .setId(Identifier.newUuid())
            .build();
    private static final CmdStopProject stopProject = CmdStopProject
            .newBuilder()
            .setId(Identifier.newUuid())
            .build();

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandsTest.class);

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Commands.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(FileDescriptor.class, DEFAULT_FILE_DESCRIPTOR)
                .setDefault(Timestamp.class, getCurrentTime())
                .setDefault(Duration.class, Durations2.ZERO)
                .setDefault(Command.class, requestFactory.createCommand(createProject, minutesAgo(1)))
                .setDefault(CommandContext.class, requestFactory.createCommandContext())
                .setDefault(ZoneOffset.class, ZoneOffsets.utc())
                .setDefault(UserId.class, GivenUserId.newUuid())
                .testStaticMethods(Commands.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    @DisplayName("sort given commands by timestamp")
    void sortByTimestamp() {
        Command cmd1 = requestFactory.createCommand(createProject, minutesAgo(1));
        Command cmd2 = requestFactory.createCommand(startProject, secondsAgo(30));
        Command cmd3 = requestFactory.createCommand(stopProject, secondsAgo(5));
        List<Command> sortedCommands = newArrayList(cmd1, cmd2, cmd3);
        List<Command> commandsToSort = newArrayList(cmd3, cmd1, cmd2);
        assertNotEquals(sortedCommands, commandsToSort);

        Commands.sort(commandsToSort);

        assertEquals(sortedCommands, commandsToSort);
    }

    @Test
    @DisplayName("check if command contexts have same actor and tenant id")
    void checkSameActorAndTenantId() {
        ActorContext.Builder actorContext = ActorContext
                .newBuilder()
                .setActor(GivenUserId.newUuid())
                .setTenantId(newUuid());
        CommandContext c1 = CommandContext
                .newBuilder()
                .setActorContext(actorContext)
                .build();
        CommandContext c2 = CommandContext
                .newBuilder()
                .setActorContext(actorContext)
                .build();

        assertTrue(sameActorAndTenant(c1, c2));
    }

    @Test
    @DisplayName("generate command id")
    void generateCommandId() {
        CommandId id = Commands.generateId();
        assertFalse(Identifier.toString(id)
                              .isEmpty());
    }

    @Test
    @DisplayName("extract message from given command")
    void extractMessage() {
        CommandMessage message = TestCommandMessage
                .newBuilder()
                .setId(Identifier.newUuid())
                .build();
        Command command = requestFactory.createCommand(message);
        assertEquals(message, Commands.getMessage(command));
    }

    @Nested
    @DisplayName("create command predicate of type")
    class CreatePredicate {

        @Test
        @DisplayName("`wereAfter`")
        void wereAfter() {
            Command command = requestFactory.command()
                                            .create(stopProject);
            assertTrue(Commands.wereAfter(secondsAgo(5))
                               .test(command));
        }

        @Test
        @DisplayName("`wereBetween`")
        void wereBetween() {
            Command fiveMinsAgo = requestFactory.createCommand(createProject, minutesAgo(5));
            Command twoMinsAgo = requestFactory.createCommand(startProject, minutesAgo(2));
            Command thirtySecondsAgo = requestFactory.createCommand(stopProject, secondsAgo(30));
            Command twentySecondsAgo = requestFactory.createCommand(stopProject, secondsAgo(20));
            Command fiveSecondsAgo = requestFactory.createCommand(stopProject, secondsAgo(5));

            long filteredCommands = Stream.of(fiveMinsAgo,
                                              twoMinsAgo,
                                              thirtySecondsAgo,
                                              twentySecondsAgo,
                                              fiveSecondsAgo)
                                          .filter(wereWithinPeriod(minutesAgo(3), secondsAgo(10)))
                                          .count();
            assertEquals(3, filteredCommands);
        }
    }

    @Test
    @DisplayName("consider command scheduled when command delay is set")
    void recognizeScheduled() {
        CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(10));
        Command cmd = requestFactory.command()
                                    .createBasedOnContext(createProject, context);
        assertTrue(Commands.isScheduled(cmd));
    }

    @Test
    @DisplayName("consider command not scheduled when no scheduling options are present")
    void recognizeNotScheduled() {
        Command cmd = requestFactory.createCommand(createProject);
        assertFalse(Commands.isScheduled(cmd));
    }

    @Test
    @DisplayName("throw exception when command delay set to negative")
    void throwOnNegativeDelay() {
        CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(-10));
        Command cmd =
                requestFactory.command()
                              .createBasedOnContext(createProject, context);
        assertThrows(IllegalArgumentException.class, () -> Commands.isScheduled(cmd));
    }

    @Test
    @DisplayName("provide stringifier for command id")
    void provideStringifierForId() {
        CommandId id = Commands.generateId();

        String str = Stringifiers.toString(id);
        CommandId convertedBack = Stringifiers.fromString(str, CommandId.class);
        assertEquals(id, convertedBack);
    }

    @Test
    @DisplayName("throw exception if checked command id is empty")
    void throwOnEmptyId() {
        assertThrows(IllegalArgumentException.class,
                     () -> Commands.checkValid(CommandId.getDefaultInstance()));
    }

    @Test
    @DisplayName("return command id value when checked")
    void returnIdWhenChecked() {
        CommandId id = Commands.generateId();
        assertEquals(id, Commands.checkValid(id));
    }

    @Test
    @DisplayName("obtain type of given command")
    void getCommandType() {
        Command command = requestFactory.generateCommand();

        TypeName typeName = CommandEnvelope.of(command)
                                           .getTypeName();
        assertNotNull(typeName);
        assertEquals(TypeName.of(TestCommandMessage.class), typeName);
    }

    @Test
    @DisplayName("obtain type url of given command")
    void getCommandTypeUrl() {
        ActorRequestFactory factory =
                TestActorRequestFactory.newInstance(CommandsTest.class);
        CommandMessage message = TestCommandMessage
                .newBuilder()
                .setId(Identifier.newUuid())
                .build();
        Command command = factory.command()
                                 .create(message);

        TypeUrl typeUrl = CommandEnvelope.of(command)
                                         .getTypeName()
                                         .toUrl();

        assertEquals(TypeUrl.of(TestCommandMessage.class), typeUrl);
    }
}
