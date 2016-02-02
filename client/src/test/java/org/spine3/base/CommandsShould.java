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
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.test.TestCommandFactory;
import org.spine3.test.Tests;
import org.spine3.time.ZoneOffsets;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.spine3.protobuf.Timestamps.minutesAgo;
import static org.spine3.protobuf.Timestamps.secondsAgo;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandsShould {

    private final TestCommandFactory commandFactory = TestCommandFactory.newInstance("CommandsShould", ZoneOffsets.UTC);

    @Test
    public void sort() {
        final Command command1 = commandFactory.create(StringValue.getDefaultInstance(), minutesAgo(1));
        final Command command2 = commandFactory.create(Int64Value.getDefaultInstance(), secondsAgo(30));
        final Command command3 = commandFactory.create(BoolValue.getDefaultInstance(), secondsAgo(5));

        final Collection<Command> sortedList = new ArrayList<>();
        sortedList.add(command1);
        sortedList.add(command2);
        sortedList.add(command3);

        final List<Command> unSortedList = new ArrayList<>();
        unSortedList.add(command3);
        unSortedList.add(command1);
        unSortedList.add(command2);

        assertFalse(sortedList.equals(unSortedList));

        Commands.sort(unSortedList);

        assertEquals(sortedList, unSortedList);
    }

    @SuppressWarnings("MethodWithTooExceptionsDeclared")
    @Test
    public void have_private_ctor()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Tests.callPrivateUtilityConstructor(Commands.class);
    }

    @Test
    public void extract_message_from_command() {
        final StringValue message = StringValue.newBuilder().setValue("extract_message_from_command").build();
        final Command command = Commands.newCommand(message, CommandContext.getDefaultInstance());
        assertEquals(message, Commands.getMessage(command));
    }

    @Test
    public void create_wereAfter_predicate() {
        final Command command = commandFactory.create(BoolValue.getDefaultInstance());
        assertTrue(Commands.wereAfter(secondsAgo(5)).apply(command));
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

        assertEquals(3, FluentIterable.from(filter).size());
    }
}
