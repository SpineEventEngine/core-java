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

package org.spine3.server.command;

import com.google.protobuf.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.testdata.TestCommands;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.milliseconds;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestCommands.addTaskMsg;
import static org.spine3.testdata.TestCommands.createProjectMsg;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ExecutorCommandSchedulerShould {

    private static final long DELAY_MS = 1100;

    private static final Duration DELAY = milliseconds(DELAY_MS);

    private CommandScheduler scheduler;
    private CommandContext context;

    @Before
    public void setUpTest() {
        scheduler = spy(ExecutorCommandScheduler.class);
        context = createCommandContext(DELAY);
    }

    @After
    public void tearDownTest() {
        scheduler.shutdown();
    }

    @Test
    public void schedule_command_if_delay_is_set() {
        final Command cmd = Commands.create(createProjectMsg(newUuid()), context);

        scheduler.schedule(cmd);

        verify(scheduler, never()).post(cmd);
        verify(scheduler, after(DELAY_MS).times(1)).post(cmd);
    }

    @Test
    public void not_schedule_command_with_same_id_twice() {
        final String id = newUuid();
        final Command expectedCmd = Commands.create(createProjectMsg(id), context);
        final Command extraCmd = Commands.create(addTaskMsg(id), context);

        scheduler.schedule(expectedCmd);
        scheduler.schedule(extraCmd);

        verify(scheduler, after(DELAY_MS).times(1)).post(expectedCmd);
        verify(scheduler, never()).post(extraCmd);
    }

    @Test
    public void throw_exception_if_is_shutdown() {
        scheduler.shutdown();
        try {
            scheduler.schedule(TestCommands.createProjectCmd());
        } catch (IllegalStateException expected) {
            // is OK as it is shutdown
            return;
        }
        fail("Must throw an exception as it is shutdown.");
    }
}
