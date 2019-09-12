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

import com.google.common.truth.Correspondence;
import com.google.common.truth.Correspondence.BinaryPredicate;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.given.CommandHandlerTestEnv.TestCommandHandler;
import io.spine.server.commandbus.given.MemoizingCommandFlowWatcher;
import io.spine.server.commandbus.given.ThreadPoolExecutors.NoOpScheduledThreadPoolExecutor;
import io.spine.server.commandbus.given.ThreadPoolExecutors.ThrowingThreadPoolExecutor;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenCommandContext;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.Durations2.milliseconds;
import static io.spine.server.BoundedContextBuilder.assumingTests;
import static io.spine.server.commandbus.Given.CommandMessage.addTask;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("ExecutorCommandScheduler should")
class ExecutorCommandSchedulerTest {

    private static final long DELAY_MS = 1100;

    private static final Duration DELAY = milliseconds(DELAY_MS);

    // Wait a bit longer to ensure the command was processed.
    private static final int WAIT_FOR_PROPAGATION_MS = 300;

    private final CommandFactory commandFactory =
            new TestActorRequestFactory(ExecutorCommandSchedulerTest.class).command();

    private CommandBus commandBus;
    private CommandScheduler scheduler;
    private MemoizingCommandFlowWatcher watcher;
    private CommandContext commandContext;
    private BoundedContext context;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        ScheduledExecutorService executorService = new NoOpScheduledThreadPoolExecutor();
        scheduler = new ExecutorCommandScheduler(executorService);
        commandContext = GivenCommandContext.withScheduledDelayOf(DELAY);

        context = assumingTests()
                .addCommandDispatcher(new TestCommandHandler())
                .build();
        commandBus = context.commandBus();

        watcher = new MemoizingCommandFlowWatcher();
        scheduler.setCommandBus(commandBus);
        scheduler.setWatcher(watcher);
    }

    @AfterEach
    void tearDown() throws Exception {
        scheduler.shutdown();
        context.close();
    }

    @Test
    @DisplayName("schedule command if delay is set")
    void scheduleCmdIfDelaySet() {
        Command command =
                commandFactory.createBasedOnContext(createProjectMessage(), commandContext);
        scheduler.schedule(command);

        assertScheduledExactly(command);
    }

    @Test
    @DisplayName("not schedule command with same ID twice")
    void notScheduleCmdWithSameId() {
        String id = newUuid();

        Command firstCommand = commandFactory.createBasedOnContext(createProjectMessage(id),
                                                                   commandContext);

        Command extraCommand = commandFactory.createBasedOnContext(addTask(id), commandContext)
                                             .toBuilder()
                                             .setId(firstCommand.getId())
                                             .build();

        scheduler.schedule(firstCommand);
        scheduler.schedule(extraCommand);

        assertScheduledExactly(firstCommand);
    }

    @Test
    @MuteLogging
    @DisplayName("continue scheduling commands after error in `post`")
    void recoverFromPostFail() throws Exception {

        // Inject a throwing executor service so the `post` operation fails.
        ScheduledExecutorService service = Executors.newScheduledThreadPool(5);
        ThrowingThreadPoolExecutor throwingExecutor = new ThrowingThreadPoolExecutor(service);
        ExecutorCommandScheduler scheduler = new ExecutorCommandScheduler(throwingExecutor);

        scheduler.setCommandBus(commandBus);
        scheduler.setWatcher(watcher);

        Command cmd1 =
                commandFactory.createBasedOnContext(createProjectMessage(), this.commandContext);
        scheduler.schedule(cmd1);

        waitForCommandProcessed();

        assertThat(throwingExecutor.throwScheduled())
                .isTrue();

        Command cmd2 =
                commandFactory.createBasedOnContext(createProjectMessage(), this.commandContext);
        scheduler.schedule(cmd2);

        assertScheduled(cmd2);

        // Wait for the second command to be "processed" to avoid test output pollution.
        waitForCommandProcessed();
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

    private void assertScheduledExactly(Command expected) {
        assertThat(watcher.scheduled())
                .hasSize(1);

        Command scheduled = watcher.scheduled()
                                   .get(0);
        Command expectedCmd =
                CommandScheduler.setSchedulingTime(expected, getSchedulingTime(scheduled));
        assertThat(scheduled)
                .isEqualTo(expectedCmd);
    }

    private void assertScheduled(Command expected) {
        BinaryPredicate<Command, Command> isScheduled =
                ExecutorCommandSchedulerTest::isEqualToScheduled;
        Correspondence<Command, Command> correspondence =
                Correspondence.from(isScheduled, "is the scheduled command");
        assertThat(watcher.scheduled())
                .comparingElementsUsing(correspondence)
                .contains(expected);
    }

    private static boolean isEqualToScheduled(Command scheduled, Command command) {
        Command withSchedulingTime =
                CommandScheduler.setSchedulingTime(command, getSchedulingTime(scheduled));
        return withSchedulingTime.equals(scheduled);
    }

    private static Timestamp getSchedulingTime(Command cmd) {
        Timestamp time = cmd.getSystemProperties()
                            .getSchedulingTime();
        return time;
    }

    private static void waitForCommandProcessed() throws InterruptedException {
        Thread.sleep(DELAY_MS + WAIT_FOR_PROPAGATION_MS);
    }
}
