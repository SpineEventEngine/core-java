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
package org.spine3.server.util;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.client.Commands;
import org.spine3.protobuf.Durations;
import org.spine3.protobuf.Timestamps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.testdata.TestCommandFactory.createProject;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandUtilShould {

    @Test
    public void sort() {

        final Timestamp minuteAgo = Timestamps.minuteAgo();
        final Timestamp thirtySecondsAgo = TimeUtil.add(TimeUtil.getCurrentTime(), Durations.ofSeconds(-30));
        final Timestamp fiveSecondsAgo = TimeUtil.add(TimeUtil.getCurrentTime(), Durations.ofSeconds(-5));

        final Command commandRequest1 = createProject(minuteAgo);
        final Command commandRequest2 = createProject(thirtySecondsAgo);
        final Command commandRequest3 = createProject(fiveSecondsAgo);

        final Collection<Command> sortedList = new ArrayList<>();
        sortedList.add(commandRequest1);
        sortedList.add(commandRequest2);
        sortedList.add(commandRequest3);

        final List<Command> unSortedList = new ArrayList<>();
        unSortedList.add(commandRequest3);
        unSortedList.add(commandRequest1);
        unSortedList.add(commandRequest2);

        assertFalse(sortedList.equals(unSortedList));

        Commands.sort(unSortedList);

        assertEquals(sortedList, unSortedList);
    }

}
