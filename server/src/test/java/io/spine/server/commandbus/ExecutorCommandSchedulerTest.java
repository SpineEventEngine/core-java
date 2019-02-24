/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenCommandContext;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.Durations2.milliseconds;
import static io.spine.server.commandbus.Given.CommandMessage.addTask;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DisplayName("ExecutorCommandScheduler should")
class ExecutorCommandSchedulerTest {

    private static final long DELAY_MS = 1100;

    private static final Duration DELAY = milliseconds(DELAY_MS);

    // Wait a bit longer in the verifier to ensure the command was processed.
    private static final int WAIT_FOR_PROPAGATION_MS = 300;

    private final CommandFactory commandFactory =
            new TestActorRequestFactory(ExecutorCommandSchedulerTest.class).command();

    private CommandBus commandBus;
    private CommandScheduler scheduler;
    private CommandContext context;

    @BeforeEach
    void setUp() {
        scheduler = spy(ExecutorCommandScheduler.class);
        context = GivenCommandContext.withScheduledDelayOf(DELAY);

        commandBus = mock(CommandBus.class);
        scheduler.setCommandBus(commandBus);

        // System BC integration is NOT tested in this suite.
        SystemWriteSide systemWriteSide = NoOpSystemWriteSide.INSTANCE;
        CommandFlowWatcher flowWatcher = new CommandFlowWatcher((t) -> systemWriteSide);
        scheduler.setFlowWatcher(flowWatcher);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    @DisplayName("schedule command if delay is set")
    void scheduleCmdIfDelaySet() {
        Command cmdPrimary =
                commandFactory.createBasedOnContext(createProjectMessage(), context);
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);

        scheduler.schedule(cmdPrimary);

        verify(scheduler, never()).post(any(Command.class));
        verify(scheduler,
               timeout(DELAY_MS + WAIT_FOR_PROPAGATION_MS)).post(commandCaptor.capture());
        Command actualCmd = commandCaptor.getValue();
        Command expectedCmd =
                CommandScheduler.setSchedulingTime(cmdPrimary, getSchedulingTime(actualCmd));
        assertEquals(expectedCmd, actualCmd);
    }

    @Test
    @DisplayName("not schedule command with same ID twice")
    void notScheduleCmdWithSameId() {
        String id = newUuid();

        Command expectedCmd = commandFactory.createBasedOnContext(createProjectMessage(id),
                                                                  context);

        Command extraCmd = commandFactory.createBasedOnContext(addTask(id), context)
                                         .toBuilder()
                                         .setId(expectedCmd.getId())
                                         .build();

        scheduler.schedule(expectedCmd);
        scheduler.schedule(extraCmd);

        verify(scheduler, timeout(DELAY_MS + WAIT_FOR_PROPAGATION_MS)).post(any(Command.class));
        verify(scheduler, never()).post(extraCmd);
    }

    @Test
    @MuteLogging
    @DisplayName("continue scheduling commands after error in `post`")
    void recoverFromPostFail() {
        doThrow(new IllegalStateException("Post failed"))
                .when(commandBus)
                .postPreviouslyScheduled(any());
        Command cmd1 =
                commandFactory.createBasedOnContext(createProjectMessage(), context);
        scheduler.schedule(cmd1);
        verify(scheduler, timeout(DELAY_MS + WAIT_FOR_PROPAGATION_MS)).post(any(Command.class));

        doCallRealMethod()
                .when(commandBus)
                .postPreviouslyScheduled(any());
        Command cmd2 =
                commandFactory.createBasedOnContext(createProjectMessage(), context);
        scheduler.schedule(cmd2);
        verify(commandBus, timeout(DELAY_MS + WAIT_FOR_PROPAGATION_MS)).dispatch(any());
    }

    @Test
    @DisplayName("throw ISE on scheduling attempt when is shutdown")
    void throwExceptionIfIsShutdown() {
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
        Timestamp time = cmd.getSystemProperties()
                            .getSchedulingTime();
        return time;
    }
}
