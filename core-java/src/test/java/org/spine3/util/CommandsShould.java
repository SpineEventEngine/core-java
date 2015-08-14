/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.util;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.testutil.CommandRequestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.spine3.util.ListFilters.*;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandsShould {

    @Test
    public void generate() {
        UserId userId = UserIds.create("commands-should-test");

        CommandId result = Commands.generateId(userId);

        //noinspection DuplicateStringLiteralInspection
        assertThat(result, allOf(
                hasProperty("actor", equalTo(userId)),
                hasProperty("timestamp")));
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_null_parameter() {
        //noinspection ConstantConditions
        Commands.generateId(null);
    }

    @Test
    public void convert_field_name_to_method_name() {
        assertEquals("getUserId", Messages.toAccessorMethodName("user_id"));
        assertEquals("getId", Messages.toAccessorMethodName("id"));
        assertEquals("getAggregateRootId", Messages.toAccessorMethodName("aggregate_root_id"));
    }

    @Test
    public void return_correct_were_after_predicate() {
        final Timestamp timestamp = Timestamps.now();
        final CommandRequest commandRequest = CommandRequestFactory.create(timestamp);
        final CommandRequest commandRequestAfter = CommandRequestFactory.create();

        final List<CommandRequest> commandList = ImmutableList.<CommandRequest>builder()
                .add(commandRequest)
                .add(commandRequestAfter)
                .build();

        final List<CommandRequest> filteredList = filter(commandList, Commands.wereAfter(timestamp));

        assertEquals(1, filteredList.size());
        assertEquals(commandRequestAfter, filteredList.get(0));
    }

    @Test
    public void sort() {
        final CommandRequest commandRequest1 = CommandRequestFactory.create();
        final CommandRequest commandRequest2 = CommandRequestFactory.create();
        final CommandRequest commandRequest3 = CommandRequestFactory.create();

        final Collection<CommandRequest> sortedList = new ArrayList<>();
        sortedList.add(commandRequest1);
        sortedList.add(commandRequest2);
        sortedList.add(commandRequest3);

        final List<CommandRequest> unSortedList = new ArrayList<>();
        unSortedList.add(commandRequest3);
        unSortedList.add(commandRequest1);
        unSortedList.add(commandRequest2);

        assertFalse(sortedList.equals(unSortedList));

        Commands.sort(unSortedList);

        assertEquals(sortedList, unSortedList);
    }

    @Test
    public void return_correct_were_within_period_predicate() {
        final CommandRequest commandRequest1 = CommandRequestFactory.create();
        final Timestamp from = Timestamps.now();
        final CommandRequest commandRequest2 = CommandRequestFactory.create();
        final Timestamp to = Timestamps.now();

        final List<CommandRequest> commandList = ImmutableList.<CommandRequest>builder()
                .add(commandRequest1)
                .add(commandRequest2)
                .build();

        final List<CommandRequest> filteredList = filter(commandList, Commands.wereWithinPeriod(
                from, to));

        assertEquals(1, filteredList.size());
        assertEquals(commandRequest2, filteredList.get(0));
    }
}
