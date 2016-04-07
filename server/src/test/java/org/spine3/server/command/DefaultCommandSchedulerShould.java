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
import org.spine3.test.project.command.CreateProject;

import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.testdata.TestCommands.createProject;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class DefaultCommandSchedulerShould {

    private static final Duration DELAY = seconds(1);
    private static final int CHECK_OFFSET_MS = 50;

    private CommandBus commandBus;
    private CommandScheduler scheduler;

    @Before
    public void setUpTest() {
        this.commandBus = mock(CommandBus.class);
        this.scheduler = new DefaultCommandScheduler(commandBus);
    }

    @After
    public void tearDownTest() {
        scheduler.shutdown();
    }

    @Test
    public void schedule_command_if_delay_is_set() throws InterruptedException {
        final CreateProject msg = createProject(newUuid());
        final Command cmdPrimary = Commands.create(msg, CommandContext.newBuilder()
                .setDelay(DELAY)
                .build());
        final Command cmdExpected = Commands.create(msg, CommandContext.getDefaultInstance());

        scheduler.schedule(cmdPrimary);

        assertCommandSent(cmdExpected, DELAY);
    }

    @Test
    public void schedule_command_if_sending_time_is_set() throws InterruptedException {
        final CreateProject msg = createProject(newUuid());
        final Command cmdPrimary = Commands.create(msg, CommandContext.newBuilder()
                .setSendingTime(add(getCurrentTime(), DELAY))
                .build());
        final Command cmdExpected = Commands.create(msg, CommandContext.getDefaultInstance());

        scheduler.schedule(cmdPrimary);

        assertCommandSent(cmdExpected, DELAY);
    }

    private void assertCommandSent(Command cmd, Duration delay) throws InterruptedException {
        verify(commandBus, never()).post(any(Command.class));

        Thread.sleep((delay.getSeconds() * 1000) + CHECK_OFFSET_MS);

        verify(commandBus, times(1)).post(cmd);
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
}
