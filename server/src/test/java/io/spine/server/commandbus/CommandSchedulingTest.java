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
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.testing.Tests;
import io.spine.time.Durations2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.commandbus.CommandScheduler.setSchedule;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.server.commandbus.Given.ACommand.startProject;
import static io.spine.time.Durations2.minutes;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Command scheduling mechanism should")
class CommandSchedulingTest extends AbstractCommandBusTestSuite {

    CommandSchedulingTest() {
        super(true);
    }

    @Test
    @DisplayName("store scheduled command to CommandStore and return `OK`")
    void storeScheduledCommand() {
        commandBus.register(createProjectHandler);
        Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        checkResult(cmd);
    }

    @Test
    @DisplayName("schedule command if delay is set")
    void scheduleIfDelayIsSet() {
        commandBus.register(createProjectHandler);
        Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        verify(scheduler).schedule(cmd);
    }

    @Test
    @DisplayName("not schedule command if no scheduling options are set")
    void notScheduleWithoutOptions() {
        commandBus.register(new CreateProjectHandler());

        Command command = createProject();
        commandBus.post(command, observer);

        verify(scheduler, never()).schedule(createProject());
        checkResult(command);
    }

    @Test
    @DisplayName("reschedule commands from storage")
    void rescheduleCmdsFromStorage() {
        Timestamp schedulingTime = minutesAgo(3);
        Duration delayPrimary = Durations2.fromMinutes(5);
        Duration newDelayExpected = Durations2.fromMinutes(2); // = 5 - 3
        List<Command> commandsPrimary =
                newArrayList(createProject(), addTask(), startProject());
        storeAsScheduled(commandsPrimary, delayPrimary, schedulingTime);

        commandBus.rescheduleCommands();

        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(commandsPrimary.size())).schedule(commandCaptor.capture());
        List<Command> commandsRescheduled = commandCaptor.getAllValues();
        for (Command cmd : commandsRescheduled) {
            long actualDelay = getDelaySeconds(cmd);
            Tests.assertSecondsEqual(newDelayExpected.getSeconds(), actualDelay, /*maxDiffSec=*/1);
        }
    }

    @Nested
    @DisplayName("reschedule commands from storage on build")
    class RescheduleOnBuild {

        @SuppressWarnings("CheckReturnValue")
        // OK to ignore stored command for the purpose of this test.
        @Test
        @DisplayName("in parallel if thread spawning is allowed")
        void inParallel() {
            String mainThreadName = Thread.currentThread()
                                          .getName();
            StringBuilder threadNameUponScheduling = new StringBuilder(0);
            CountDownLatch latch = new CountDownLatch(1);
            CommandScheduler scheduler =
                    threadAwareScheduler(threadNameUponScheduling, latch);
            TestSystemGateway gateway = new TestSingleTenantSystemGateway();
            storeSingleCommandForRescheduling(gateway);

            // Create CommandBus specific for this test.
            CommandBus commandBus = CommandBus
                    .newBuilder()
                    .injectTenantIndex(tenantIndex)
                    .injectSystemGateway(gateway)
                    .setCommandScheduler(scheduler)
                    .setThreadSpawnAllowed(true)
                    .setAutoReschedule(true)
                    .build();
            assertNotNull(commandBus);

            // Await to ensure the commands have been rescheduled in parallel.
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }

            // Ensure the scheduler has been called for a single command,
            ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
            verify(scheduler, times(1)).schedule(commandCaptor.capture());

            // and the call has been made for a thread, different than the main thread.
            String actualThreadName = threadNameUponScheduling.toString();
            assertNotNull(actualThreadName);
            assertNotEquals(mainThreadName, actualThreadName);
        }

        @SuppressWarnings("CheckReturnValue")
        // OK to ignore stored command for the purpose of this test.
        @Test
        @DisplayName("synchronously if thread spawning is not allowed")
        void synchronously() {
            String mainThreadName = Thread.currentThread()
                                          .getName();
            StringBuilder threadNameUponScheduling = new StringBuilder(0);
            CountDownLatch latch = new CountDownLatch(1);
            CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling, latch);

            TestSystemGateway systemGateway = new TestSingleTenantSystemGateway();
            storeSingleCommandForRescheduling(systemGateway);

            // Create CommandBus specific for this test.
            CommandBus commandBus = CommandBus
                    .newBuilder()
                    .injectTenantIndex(tenantIndex)
                    .injectSystemGateway(systemGateway)
                    .setCommandScheduler(scheduler)
                    .setThreadSpawnAllowed(false)
                    .setAutoReschedule(true)
                    .build();
            assertNotNull(commandBus);

            // Ensure the scheduler has been called for a single command,
            ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
            verify(scheduler, times(1)).schedule(commandCaptor.capture());

            // and the call has been made in the main thread (as spawning is not allowed).
            String actualThreadName = threadNameUponScheduling.toString();
            assertNotNull(actualThreadName);
            assertEquals(mainThreadName, actualThreadName);
        }

        /**
         * Creates a new thread-aware scheduler spied by Mockito.
         *
         * <p>The method is not {@code static} to allow Mockito spy on the created anonymous class
         * instance.
         *
         * @param targetThreadName the builder of the thread name that will be created upon command
         *                         scheduling
         * @param latch            the instance of the {@code CountDownLatch} to await the execution
         *                         finishing
         * @return newly created instance
         */
        private CommandScheduler threadAwareScheduler(StringBuilder targetThreadName,
                                                      CountDownLatch latch) {
            return spy(new ExecutorCommandScheduler() {
                @Override
                public void schedule(Command command) {
                    super.schedule(command);
                    targetThreadName.append(Thread.currentThread()
                                                  .getName());
                    latch.countDown();
                }
            });
        }
    }

    @Test
    @DisplayName("post previously scheduled command")
    void postPreviouslyScheduled() {
        CommandBus spy = spy(commandBus);
        spy.register(createProjectHandler);
        Command command = storeSingleCommandForRescheduling();

        spy.postPreviouslyScheduled(command);

        verify(spy).dispatch(eq(CommandEnvelope.of(command)));
    }

    @Test
    @DisplayName("reject previously scheduled command if no endpoint is found")
    void rejectPreviouslyScheduledWithoutEndpoint() {
        Command command = storeSingleCommandForRescheduling();
        assertThrows(IllegalStateException.class,
                     () -> commandBus.postPreviouslyScheduled(command));
    }

    @Nested
    @DisplayName("allow updating")
    class Update {

        @Test
        @DisplayName("scheduling options")
        void schedulingOptions() {
            Command cmd = requestFactory.command()
                                        .create(toMessage(newUuid()));
            Timestamp schedulingTime = getCurrentTime();
            Duration delay = Durations2.minutes(5);

            Command cmdUpdated = setSchedule(cmd, delay, schedulingTime);
            CommandContext.Schedule schedule = cmdUpdated.getContext()
                                                         .getSchedule();

            assertEquals(delay, schedule.getDelay());
            assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                                   .getSchedulingTime());
        }

        @Test
        @DisplayName("scheduling time")
        void schedulingTime() {
            Command cmd = requestFactory.command()
                                        .create(toMessage(newUuid()));
            Timestamp schedulingTime = getCurrentTime();

            Command cmdUpdated = CommandScheduler.setSchedulingTime(cmd, schedulingTime);

            assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                                   .getSchedulingTime());
        }
    }

    /*
     * Utility methods
     ********************/

    private static Command createScheduledCommand() {
        Timestamp schedulingTime = minutesAgo(3);
        Duration delayPrimary = Durations2.fromMinutes(5);
        return setSchedule(createProject(), delayPrimary, schedulingTime);
    }

    /**
     * Creates and stores a single scheduled command.
     */
    private Command storeSingleCommandForRescheduling() {
        return storeSingleCommandForRescheduling(systemGateway);
    }

    /**
     * Creates and stores a single scheduled command.
     */
    private static Command storeSingleCommandForRescheduling(TestSystemGateway gateway) {
        Command cmdWithSchedule = createScheduledCommand();
        gateway.schedule(cmdWithSchedule);
        return cmdWithSchedule;
    }

    private static long getDelaySeconds(Command cmd) {
        long delaySec = cmd.getContext()
                           .getSchedule()
                           .getDelay()
                           .getSeconds();
        return delaySec;
    }
}
