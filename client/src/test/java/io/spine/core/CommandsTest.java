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

package io.spine.core;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.given.GivenCommandContext;
import io.spine.core.given.GivenUserId;
import io.spine.string.Stringifiers;
import io.spine.time.Durations2;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.Descriptors.FileDescriptor;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Commands.sameActorAndTenant;
import static io.spine.core.given.GivenTenantId.newUuid;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.TimeTests.Past.minutesAgo;
import static io.spine.test.TimeTests.Past.secondsAgo;
import static io.spine.time.Durations2.seconds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@linkplain Commands Commands utility class}.
 *
 * <p>The test suite is located under the "client" module since actor request generation
 * is required. So we want to avoid circular dependencies between "core" and "client" modules.
 *
 * @author Alexander Yevsyukov
 */
@DisplayName("Commands utility should")
class CommandsTest {

    private static final FileDescriptor DEFAULT_FILE_DESCRIPTOR = Any.getDescriptor()
                                                                     .getFile();

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandsTest.class);

    @SuppressWarnings("DuplicateStringLiteralInspection") // Display name for utility c-tor test.
    @Test
    @DisplayName("have private parameterless constructor")
    void haveUtilityCtor() {
        assertHasPrivateParameterlessCtor(Commands.class);
    }

    @Test
    @DisplayName("sort given commands by timestamp")
    void sortByTimestamp() {
        final Command cmd1 = requestFactory.createCommand(StringValue.getDefaultInstance(),
                                                          minutesAgo(1));
        final Command cmd2 = requestFactory.createCommand(Int64Value.getDefaultInstance(),
                                                          secondsAgo(30));
        final Command cmd3 = requestFactory.createCommand(BoolValue.getDefaultInstance(),
                                                          secondsAgo(5));
        final List<Command> sortedCommands = newArrayList(cmd1, cmd2, cmd3);
        final List<Command> commandsToSort = newArrayList(cmd3, cmd1, cmd2);
        assertFalse(sortedCommands.equals(commandsToSort));

        Commands.sort(commandsToSort);

        assertEquals(sortedCommands, commandsToSort);
    }

    @Test
    @DisplayName("check if command contexts have same actor and tenantId")
    void checkSameActorAndTenantId() {
        final ActorContext.Builder actorContext =
                ActorContext.newBuilder()
                            .setActor(GivenUserId.newUuid())
                            .setTenantId(newUuid());
        final CommandContext c1 =
                CommandContext.newBuilder()
                              .setActorContext(actorContext)
                              .build();
        final CommandContext c2 =
                CommandContext.newBuilder()
                              .setActorContext(actorContext)
                              .build();

        assertTrue(sameActorAndTenant(c1, c2));
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Display name for null test.
    @Test
    @DisplayName("not accept nulls for non-Nullable public method arguments")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(FileDescriptor.class, DEFAULT_FILE_DESCRIPTOR)
                .setDefault(Timestamp.class, getCurrentTime())
                .setDefault(Duration.class, Durations2.ZERO)
                .setDefault(Command.class,
                            requestFactory.createCommand(StringValue.getDefaultInstance(),
                                                         minutesAgo(1)))
                .setDefault(CommandContext.class, requestFactory.createCommandContext())
                .setDefault(ZoneOffset.class, ZoneOffsets.UTC)
                .setDefault(UserId.class, GivenUserId.newUuid())
                .testStaticMethods(Commands.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    @DisplayName("generate command ids")
    void generateCommandId() {
        final CommandId id = Commands.generateId();

        assertFalse(Identifier.toString(id)
                              .isEmpty());
    }

    @Test
    @DisplayName("extract message from given command")
    void extractMessage() {
        final StringValue message = toMessage("extract_message_from_command");

        final Command command = requestFactory.createCommand(message);
        assertEquals(message, Commands.getMessage(command));
    }

    @Nested
    @DisplayName("when creating predicate for commands")
    class PredicatesCreationTest {

        @Test
        @DisplayName("support `wereAfter` predicate")
        void createWereAfterPredicate() {
            final Command command = requestFactory.command()
                                                  .create(BoolValue.getDefaultInstance());
            assertTrue(Commands.wereAfter(secondsAgo(5))
                               .apply(command));
        }

        @Test
        @DisplayName("support `wereBetween` predicate")
        void createWereBetweenPredicate() {
            final Command command1 = requestFactory.createCommand(StringValue.getDefaultInstance(),
                                                                  minutesAgo(5));
            final Command command2 = requestFactory.createCommand(Int64Value.getDefaultInstance(),
                                                                  minutesAgo(2));
            final Command command3 = requestFactory.createCommand(BoolValue.getDefaultInstance(),
                                                                  secondsAgo(30));
            final Command command4 = requestFactory.createCommand(BoolValue.getDefaultInstance(),
                                                                  secondsAgo(20));
            final Command command5 = requestFactory.createCommand(BoolValue.getDefaultInstance(),
                                                                  secondsAgo(5));

            final ImmutableList<Command> commands =
                    ImmutableList.of(command1, command2, command3, command4, command5);
            final Iterable<Command> filter = Iterables.filter(
                    commands,
                    Commands.wereWithinPeriod(minutesAgo(3), secondsAgo(10))
            );

            assertEquals(3, FluentIterable.from(filter)
                                          .size());
        }
    }

    @Nested
    @DisplayName("when evaluating if command is scheduled")
    class CommandScheduledTest {

        @Test
        @DisplayName("consider command scheduled when command delay is set")
        void recognizeScheduled() {
            final CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(10));
            final Command cmd = requestFactory.command()
                                              .createBasedOnContext(
                                                      StringValue.getDefaultInstance(),
                                                      context);
            assertTrue(Commands.isScheduled(cmd));
        }

        @Test
        @DisplayName("consider command not scheduled when no scheduling options are present")
        void recognizeNotScheduled() {
            final Command cmd = requestFactory.createCommand(StringValue.getDefaultInstance());
            assertFalse(Commands.isScheduled(cmd));
        }

        @Test
        @DisplayName("throw exception when command delay set to negative")
        void throwOnNegativeDelay() {
            final CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(-10));
            final Command cmd = requestFactory.command()
                                              .createBasedOnContext(StringValue.getDefaultInstance(),
                                                                    context);
            assertThrows(IllegalArgumentException.class, () -> Commands.isScheduled(cmd));
        }
    }

    @Test
    @DisplayName("provide stringifier for CommandId")
    void provideStringifierForId() {
        final CommandId id = Commands.generateId();

        final String str = Stringifiers.toString(id);
        final CommandId convertedBack = Stringifiers.fromString(str, CommandId.class);
        assertEquals(id, convertedBack);
    }

    @Nested
    @DisplayName("when checking if command is valid")
    class CheckValidTest {

        @Test
        @DisplayName("throw exception if checked command id is empty")
        void throwOnEmptyId() {
            assertThrows(IllegalArgumentException.class,
                         () -> Commands.checkValid(CommandId.getDefaultInstance()));
        }

        @Test
        @DisplayName("return command id value when checked")
        void returnIdWhenChecked() {
            final CommandId id = Commands.generateId();
            assertEquals(id, Commands.checkValid(id));
        }
    }

    @Test
    @DisplayName("obtain type of given command")
    void getCommandType() {
        final Command command = requestFactory.generateCommand();

        final TypeName typeName = CommandEnvelope.of(command)
                                                 .getTypeName();
        assertNotNull(typeName);
        assertEquals(StringValue.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    @DisplayName("obtain type url of given command")
    void getCommandTypeUrl() {
        final ActorRequestFactory factory =
                TestActorRequestFactory.newInstance(CommandsTest.class);
        final StringValue message = toMessage(Identifier.newUuid());
        final Command command = factory.command()
                                       .create(message);

        final TypeUrl typeUrl = CommandEnvelope.of(command)
                                               .getTypeName()
                                               .toUrl();

        assertEquals(TypeUrl.of(StringValue.class), typeUrl);
    }
}
