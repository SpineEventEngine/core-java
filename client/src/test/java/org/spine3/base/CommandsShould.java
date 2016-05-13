/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.base;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.client.test.TestCommandFactory;
import org.spine3.protobuf.Durations;
import org.spine3.test.RunTest;
import org.spine3.test.TestCommand;
import org.spine3.type.TypeName;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.Descriptors.FileDescriptor;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Timestamps.minutesAgo;
import static org.spine3.protobuf.Timestamps.secondsAgo;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

@SuppressWarnings({"InstanceMethodNamingConvention", "MagicNumber"})
public class CommandsShould {

    private final TestCommandFactory commandFactory = TestCommandFactory.newInstance(CommandsShould.class);

    @Test
    public void sort() {
        final Command cmd1 = commandFactory.create(StringValue.getDefaultInstance(), minutesAgo(1));
        final Command cmd2 = commandFactory.create(Int64Value.getDefaultInstance(), secondsAgo(30));
        final Command cmd3 = commandFactory.create(BoolValue.getDefaultInstance(), secondsAgo(5));
        final List<Command> sortedCommands = newArrayList(cmd1, cmd2, cmd3);
        final List<Command> commandsToSort = newArrayList(cmd3, cmd1, cmd2);
        assertFalse(sortedCommands.equals(commandsToSort));

        Commands.sort(commandsToSort);

        assertEquals(sortedCommands, commandsToSort);
    }

    @Test
    public void have_private_ctor() {
        assertTrue(hasPrivateUtilityConstructor(Commands.class));
    }

    @Test
    public void generate_command_ids() {
        final CommandId id = Commands.generateId();

        assertFalse(idToString(id).isEmpty());
    }

    @Test
    public void extract_message_from_command() {
        final StringValue message = newStringValue("extract_message_from_command");

        final Command command = Commands.create(message, CommandContext.getDefaultInstance());

        assertEquals(message, Commands.getMessage(command));
    }

    @Test
    public void extract_id_from_command() {
        final Command command = commandFactory.create(newStringValue(newUuid()));

        assertEquals(command.getContext().getCommandId(), getId(command));
    }

    @Test
    public void create_wereAfter_predicate() {
        final Command command = commandFactory.create(BoolValue.getDefaultInstance());
        assertTrue(Commands.wereAfter(secondsAgo(5))
                           .apply(command));
    }

    @Test
    public void create_wereBetween_predicate() {
        final Command command1 = commandFactory.create(StringValue.getDefaultInstance(), minutesAgo(5));
        final Command command2 = commandFactory.create(Int64Value.getDefaultInstance(), minutesAgo(2));
        final Command command3 = commandFactory.create(BoolValue.getDefaultInstance(), secondsAgo(30));
        final Command command4 = commandFactory.create(BoolValue.getDefaultInstance(), secondsAgo(20));
        final Command command5 = commandFactory.create(BoolValue.getDefaultInstance(), secondsAgo(5));

        final ImmutableList<Command> commands = ImmutableList.of(command1, command2, command3, command4, command5);
        final Iterable<Command> filter = Iterables.filter(commands, Commands.wereWithinPeriod(minutesAgo(3), secondsAgo(10)));

        assertEquals(3, FluentIterable.from(filter)
                                      .size());
    }

    @Test
    public void create_logging_message_for_command_with_type_and_id() {
        final Message message = RunTest.newBuilder()
                                       .setMethodName("create_logging_message_for_command_with_type_and_id")
                                       .build();
        final Command command = commandFactory.create(message);
        final String typeName = TypeName.ofCommand(command).toString();
        final String commandId = idToString(getId(command));

        @SuppressWarnings("QuestionableName") // is OK for this test.
        final String string = Commands.formatCommandTypeAndId("Command type: %s; ID %s", command);

        assertTrue(string.contains(typeName));
        assertTrue(string.contains(commandId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_create_logging_message_and_format_string_is_empty() {
        Commands.formatCommandTypeAndId("", commandFactory.create(newStringValue(newUuid())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_create_logging_message_and_format_string_is_blank() {
        Commands.formatCommandTypeAndId("  ", commandFactory.create(newStringValue(newUuid())));
    }
    
    @Test
    public void return_true_if_file_is_for_commands() {
        final FileDescriptor file = TestCommand.getDescriptor().getFile();

        assertTrue(Commands.isCommandsFile(file));
    }

    @Test
    public void return_false_if_file_is_not_for_commands() {
        final FileDescriptor file = StringValue.getDescriptor().getFile();

        assertFalse(Commands.isCommandsFile(file));
    }

    // TODO:2016-04-06:alexander.litus: implement (find out how to use entity.proto in test .proto files)
    public void return_true_if_file_belongs_to_entity() {
    }

    @Test
    public void return_false_if_file_does_not_belong_to_entity() {
        final FileDescriptor file = StringValue.getDescriptor().getFile();

        assertFalse(Commands.isEntityFile(file));
    }

    @Test
    public void when_command_delay_is_set_then_consider_it_scheduled() {
        final CommandContext context = createCommandContext(/*delay=*/seconds(10));
        final Command cmd = Commands.create(StringValue.getDefaultInstance(), context);

        assertTrue(Commands.isScheduled(cmd));
    }

    @Test
    public void when_no_scheduling_options_then_consider_command_not_scheduled() {
        final Command cmd = Commands.create(StringValue.getDefaultInstance(), CommandContext.getDefaultInstance());

        assertFalse(Commands.isScheduled(cmd));
    }

    @Test(expected = IllegalArgumentException.class)
    public void when_set_negative_delay_then_throw_exception() {
        final CommandContext context = createCommandContext(/*delay=*/seconds(-10));
        final Command cmd = Commands.create(StringValue.getDefaultInstance(), context);

        Commands.isScheduled(cmd);
    }

    @Test
    public void update_schedule_options() {
        final Command cmd = commandFactory.create(newStringValue(newUuid()));
        final Timestamp schedulingTime = getCurrentTime();
        final Duration delay = Durations.ofMinutes(5);

        final Command cmdUpdated = Commands.setSchedule(cmd, delay, schedulingTime);

        final CommandContext.Schedule schedule = cmdUpdated.getContext().getSchedule();
        assertEquals(delay, schedule.getDelay());
        assertEquals(schedulingTime, schedule.getSchedulingTime());
    }

    @Test
    public void update_scheduling_time() {
        final Command cmd = commandFactory.create(newStringValue(newUuid()));
        final Timestamp schedulingTime = getCurrentTime();

        final Command cmdUpdated = Commands.setSchedulingTime(cmd, schedulingTime);

        assertEquals(schedulingTime, cmdUpdated.getContext().getSchedule().getSchedulingTime());
    }
}
