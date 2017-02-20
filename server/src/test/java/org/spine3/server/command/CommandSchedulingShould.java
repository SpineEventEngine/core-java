/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.protobuf.Durations2;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.setSchedule;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations2.minutes;
import static org.spine3.server.command.Given.Command.addTask;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.server.command.Given.Command.startProject;
import static org.spine3.test.TimeTests.Past.minutesAgo;

public class CommandSchedulingShould extends AbstractCommandBusTestSuite {

    @Test
    public void store_scheduled_command_and_return_OK() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(cmd, SCHEDULED);
        responseObserver.assertResponseOkAndCompleted();
    }

    @Test
    public void schedule_command_if_delay_is_set() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(scheduler).schedule(cmd);
    }

    @Test
    public void not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler(newUuid()));

        commandBus.post(createProject(), responseObserver);

        verify(scheduler, never()).schedule(createProject());
        responseObserver.assertResponseOkAndCompleted();
    }

    @Test
    public void reschedule_commands_from_storage() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations2.fromMinutes(5);
        final Duration newDelayExpected = Durations2.fromMinutes(2); // = 5 - 3
        final List<Command> commandsPrimary = newArrayList(createProject(), addTask(), startProject());
        storeAsScheduled(commandsPrimary, delayPrimary, schedulingTime);

        commandBus.rescheduler().doRescheduleCommands();

        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(commandsPrimary.size())).schedule(commandCaptor.capture());
        final List<Command> commandsRescheduled = commandCaptor.getAllValues();
        for (Command cmd : commandsRescheduled) {
            final long actualDelay = getDelaySeconds(cmd);
            assertSecondsEqual(newDelayExpected.getSeconds(), actualDelay, /*maxDiffSec=*/1);
        }
    }

    @Test
    public void reschedule_commands_from_storage_in_parallel_on_build_if_thread_spawning_allowed() {
        final String mainThreadName = Thread.currentThread().getName();
        final StringBuilder threadNameUponScheduling = new StringBuilder(0);
        final CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling);
        storeSingleCommandForRescheduling();

        // Create CommandBus specific for this test.
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .setCommandScheduler(scheduler)
                                                .setThreadSpawnAllowed(true)
                                                .setAutoReschedule(true)
                                                .build();
        assertNotNull(commandBus);

        // Sleep to ensure the commands have been rescheduled in parallel.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        // Ensure the scheduler has been called for a single command,
        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(1)).schedule(commandCaptor.capture());

        // and the call has been made for a thread, different than the main thread.
        final String actualThreadName = threadNameUponScheduling.toString();
        assertNotNull(actualThreadName);
        assertNotEquals(mainThreadName, actualThreadName);
    }

    @Test
    public void reschedule_commands_from_storage_synchronously_on_build_if_thread_spawning_NOT_allowed() {
        final String mainThreadName = Thread.currentThread().getName();
        final StringBuilder threadNameUponScheduling = new StringBuilder(0);
        final CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling);
        storeSingleCommandForRescheduling();

        // Create CommandBus specific for this test.
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .setCommandScheduler(scheduler)
                                                .setThreadSpawnAllowed(false)
                                                .setAutoReschedule(true)
                                                .build();
        assertNotNull(commandBus);

        // Ensure the scheduler has been called for a single command,
        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(1)).schedule(commandCaptor.capture());

        // and the call has been made in the main thread (as spawning is not allowed).
        final String actualThreadName = threadNameUponScheduling.toString();
        assertNotNull(actualThreadName);
        assertEquals(mainThreadName, actualThreadName);
    }

    @Test
    public void post_previously_scheduled_command() {
        CommandBus spy = spy(commandBus);
        spy.register(createProjectHandler);
        Command command = storeSingleCommandForRescheduling();

        spy.postPreviouslyScheduled(command);

        verify(spy).doPost(eq(new CommandEnvelope(command)), any(CommandEndpoint.class));
    }

    @Test(expected = IllegalStateException.class)
    public void reject_previously_scheduled_command_if_no_endpoint_found() {
        Command command = storeSingleCommandForRescheduling();
        commandBus.postPreviouslyScheduled(command);
    }
    /*
     * Utility methods
     ********************/

    private static Command createScheduledCommand() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations2.fromMinutes(5);
        return setSchedule(createProject(), delayPrimary, schedulingTime);
    }

    /**
     * Creates and stores one scheduled command.
     */
    private Command storeSingleCommandForRescheduling() {
        final Command cmdWithSchedule = createScheduledCommand();
        commandStore.store(cmdWithSchedule, SCHEDULED);
        return cmdWithSchedule;
    }

    /**
     * Creates a new thread-aware scheduler spied by Mockito.
     *
     * <p>The method is not {@code static} to allow Mockito spy on the created anonymous class instance.
     *
     * @param targetThreadName the builder of the thread name that will be created upon command scheduling
     * @return newly created instance
     */
    @SuppressWarnings("MethodMayBeStatic") // see Javadoc.
    private CommandScheduler threadAwareScheduler(final StringBuilder targetThreadName) {
        return spy(new ExecutorCommandScheduler() {
            @Override
            public void schedule(Command command) {
                super.schedule(command);
                targetThreadName.append(Thread.currentThread().getName());
            }
        });
    }

    private void storeAsScheduled(Iterable<Command> commands, Duration delay, Timestamp schedulingTime) {
        for (Command cmd : commands) {
            final Command cmdWithSchedule = setSchedule(cmd, delay, schedulingTime);
            commandStore.store(cmdWithSchedule, SCHEDULED);
        }
    }

    private static void assertSecondsEqual(long expectedSec, long actualSec, long maxDiffSec) {
        final long diffSec = abs(expectedSec - actualSec);
        assertTrue(diffSec <= maxDiffSec);
    }

    private static long getDelaySeconds(Command cmd) {
        final long delaySec = cmd
                .getContext()
                .getSchedule()
                .getDelay()
                .getSeconds();
        return delaySec;
    }
}
