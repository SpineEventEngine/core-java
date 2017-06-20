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

package io.spine.base;

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
import io.spine.Identifier;
import io.spine.base.given.GivenCommandContext;
import io.spine.client.ActorRequestFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.envelope.CommandEnvelope;
import io.spine.protobuf.Wrapper;
import io.spine.string.Stringifiers;
import io.spine.test.Values;
import io.spine.test.commands.TestCommand;
import io.spine.time.Durations2;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.Descriptors.FileDescriptor;
import static io.spine.Identifier.newUuid;
import static io.spine.base.Commands.sameActorAndTenant;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.TimeTests.Past.minutesAgo;
import static io.spine.test.TimeTests.Past.secondsAgo;
import static io.spine.test.Values.newTenantUuid;
import static io.spine.test.Values.newUserUuid;
import static io.spine.test.Values.newUuidValue;
import static io.spine.time.Durations2.seconds;
import static io.spine.time.Time.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CommandsShould {

    private static final FileDescriptor DEFAULT_FILE_DESCRIPTOR = Any.getDescriptor()
                                                                     .getFile();

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandsShould.class);

    @Test
    public void have_private_ctor() {
        assertHasPrivateParameterlessCtor(Commands.class);
    }

    @Test
    public void sort_commands_by_timestamp() {
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
    public void check_if_contexts_have_same_actor_and_tenantId() {
        final ActorContext.Builder actorContext =
                ActorContext.newBuilder()
                            .setActor(newUserUuid())
                            .setTenantId(newTenantUuid());
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

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(FileDescriptor.class, DEFAULT_FILE_DESCRIPTOR)
                .setDefault(Timestamp.class, getCurrentTime())
                .setDefault(Duration.class, Durations2.ZERO)
                .setDefault(Command.class,
                            requestFactory.createCommand(StringValue.getDefaultInstance(),
                                                         minutesAgo(1)))
                .setDefault(CommandContext.class, requestFactory.createCommandContext())
                .setDefault(ZoneOffset.class, ZoneOffsets.UTC)
                .setDefault(UserId.class, Values.newUserUuid())
                .testStaticMethods(Commands.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void generate_command_ids() {
        final CommandId id = Commands.generateId();

        assertFalse(Identifier.toString(id).isEmpty());
    }

    @Test
    public void extract_message_from_command() {
        final StringValue message = Wrapper.forString("extract_message_from_command");

        final Command command = requestFactory.createCommand(message);
        assertEquals(message, Commands.getMessage(command));
    }

    @Test
    public void create_wereAfter_predicate() {
        final Command command = requestFactory.command()
                                              .create(BoolValue.getDefaultInstance());
        assertTrue(Commands.wereAfter(secondsAgo(5))
                           .apply(command));
    }

    @Test
    public void create_wereBetween_predicate() {
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

    @Test
    public void return_true_if_file_is_for_commands() {
        final FileDescriptor file = TestCommand.getDescriptor()
                                               .getFile();

        assertTrue(Commands.isCommandsFile(file));
    }

    @Test
    public void return_false_if_file_is_not_for_commands() {
        final FileDescriptor file = StringValue.getDescriptor()
                                               .getFile();

        assertFalse(Commands.isCommandsFile(file));
    }

    @Test
    public void when_command_delay_is_set_then_consider_it_scheduled() {
        final CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(10));
        final Command cmd = requestFactory.command().createBasedOnContext(
                                                             StringValue.getDefaultInstance(),
                                                             context);
        assertTrue(Commands.isScheduled(cmd));
    }

    @Test
    public void when_no_scheduling_options_then_consider_command_not_scheduled() {
        final Command cmd = requestFactory.createCommand(StringValue.getDefaultInstance());
        assertFalse(Commands.isScheduled(cmd));
    }

    @Test(expected = IllegalArgumentException.class)
    public void when_set_negative_delay_then_throw_exception() {
        final CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(-10));
        final Command cmd = requestFactory.command()
                                          .createBasedOnContext(StringValue.getDefaultInstance(),
                                                                context);
        Commands.isScheduled(cmd);
    }

    @Test
    public void provide_stringifier_for_CommandId() {
        final CommandId id = Commands.generateId();

        final String str = Stringifiers.toString(id);
        final CommandId convertedBack = Stringifiers.fromString(str, CommandId.class);
        assertEquals(id, convertedBack);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_checked_command_id_is_empty() {
        Commands.checkValid(CommandId.getDefaultInstance());
    }

    @Test
    public void return_value_id_when_checked() {
        final CommandId id = Commands.generateId();
        assertEquals(id, Commands.checkValid(id));
    }

    @Test
    public void obtain_type_of_command() {
        final Command command = requestFactory.command().create(newUuidValue());

        final TypeName typeName = CommandEnvelope.of(command)
                                                 .getTypeName();
        assertNotNull(typeName);
        assertEquals(StringValue.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    public void obtain_type_url_of_command() {
        final ActorRequestFactory factory =
                TestActorRequestFactory.newInstance(CommandsShould.class);
        final StringValue message = Wrapper.forString(newUuid());
        final Command command = factory.command().create(message);

        final TypeUrl typeUrl = CommandEnvelope.of(command)
                                               .getTypeName()
                                               .toUrl();

        assertEquals(TypeUrl.of(StringValue.class), typeUrl);
    }
}
