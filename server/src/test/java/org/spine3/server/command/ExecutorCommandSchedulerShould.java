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

import com.google.common.base.Function;
import com.google.protobuf.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;

import javax.annotation.Nullable;

import static com.google.common.base.Throwables.propagate;
import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.testdata.TestCommands.addTask;
import static org.spine3.testdata.TestCommands.createProject;
import static org.spine3.testdata.TestContextFactory.createCommandContext;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ExecutorCommandSchedulerShould {

    private static final Duration DELAY = seconds(1);
    private static final long DELAY_MS = DELAY.getSeconds() * 1000;
    private static final int CHECK_OFFSET_MS = 50;

    private CommandScheduler scheduler;

    private Command postedCommand;

    @Before
    public void setUpTest() {
        this.scheduler = new ExecutorCommandScheduler();
        scheduler.setPostFunction(new Function<Command, Command>() {
            @Nullable
            @Override
            public Command apply(@Nullable Command input) {
                postedCommand = input;
                return input;
            }
        });
    }

    @After
    public void tearDownTest() {
        scheduler.shutdown();
    }

    @Test
    public void schedule_command_if_delay_is_set() {
        final CommandContext context = createCommandContext(DELAY);
        final Command cmd = Commands.create(createProject(newUuid()), context);

        scheduler.schedule(cmd);

        assertNull(postedCommand);
        sleep(DELAY_MS + CHECK_OFFSET_MS);
        assertEquals(cmd, postedCommand);
    }

    @Test
    public void not_schedule_command_with_same_id_twice() {
        final CommandContext context = createCommandContext(DELAY);
        final String id = newUuid();
        final Command expectedCmd = Commands.create(createProject(id), context);
        final Command extraCmd = Commands.create(addTask(id), context);

        scheduler.schedule(expectedCmd);
        scheduler.schedule(extraCmd);

        sleep(DELAY_MS + CHECK_OFFSET_MS);
        assertEquals(expectedCmd, postedCommand);
    }

    @Test
    public void throw_exception_if_is_shutdown() {
        scheduler.shutdown();
        try {
            scheduler.schedule(createProject());
        } catch (IllegalStateException expected) {
            // is OK as it is shutdown
            return;
        }
        fail("Must throw an exception as it is shutdown.");
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            throw propagate(e);
        }
    }
}
