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

package io.spine.server.commandbus;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.client.CommandFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.given.GivenCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.spine.Identifier.newUuid;
import static io.spine.server.commandbus.Given.CommandMessage.addTask;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.time.Durations2.milliseconds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 */
public class ExecutorCommandSchedulerShould {

    private static final long DELAY_MS = 1100;

    private static final Duration DELAY = milliseconds(DELAY_MS);

    // Wait a bit longer in the verifier to ensure the command was processed.
    private static final int WAIT_FOR_PROPAGATION_MS = 300;

    private final CommandFactory commandFactory =
            TestActorRequestFactory.newInstance(ExecutorCommandSchedulerShould.class).command();

    private CommandScheduler scheduler;
    private CommandContext context;

    @Before
    public void setUp() {
        scheduler = spy(ExecutorCommandScheduler.class);
        context = GivenCommandContext.withScheduledDelayOf(DELAY);
    }

    @After
    public void tearDown() {
        scheduler.shutdown();
    }

    @Test
    public void schedule_command_if_delay_is_set() {
        final Command cmdPrimary =
                commandFactory.createBasedOnContext(createProjectMessage(), context);
        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);

        scheduler.schedule(cmdPrimary);

        verify(scheduler, never()).post(any(Command.class));
        verify(scheduler, timeout(DELAY_MS + WAIT_FOR_PROPAGATION_MS)).post(commandCaptor.capture());
        final Command actualCmd = commandCaptor.getValue();
        final Command expectedCmd = CommandScheduler.setSchedulingTime(cmdPrimary, getSchedulingTime(actualCmd));
        assertEquals(expectedCmd, actualCmd);
    }

    @Test
    public void not_schedule_command_with_same_id_twice() {
        final String id = newUuid();

        final Command expectedCmd = commandFactory.createBasedOnContext(createProjectMessage(id),
                                                                        context);

        final Command extraCmd = commandFactory.createBasedOnContext(addTask(id), context)
                                               .toBuilder()
                                               .setId(expectedCmd.getId())
                                               .build();

        scheduler.schedule(expectedCmd);
        scheduler.schedule(extraCmd);

        verify(scheduler, timeout(DELAY_MS + WAIT_FOR_PROPAGATION_MS)).post(any(Command.class));
        verify(scheduler, never()).post(extraCmd);
    }

    @Test
    public void throw_exception_if_is_shutdown() {
        scheduler.shutdown();
        try {
            scheduler.schedule(Given.ACommand.createProject());
        } catch (IllegalStateException expected) {
            // is OK as it is shutdown
            return;
        }
        fail("Must throw an exception as it is shutdown.");
    }

    private static Timestamp getSchedulingTime(Command cmd) {
        final Timestamp time = cmd.getSystemProperties()
                                  .getSchedulingTime();
        return time;
    }
}
