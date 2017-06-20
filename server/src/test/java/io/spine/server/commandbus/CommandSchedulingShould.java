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

package io.spine.server.commandbus;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.protobuf.Wrapper;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.Tests;
import io.spine.time.Durations2;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.CommandStatus.SCHEDULED;
import static io.spine.base.Identifier.newUuid;
import static io.spine.server.commandbus.CommandScheduler.setSchedule;
import static io.spine.server.commandbus.Given.Command.addTask;
import static io.spine.server.commandbus.Given.Command.createProject;
import static io.spine.server.commandbus.Given.Command.startProject;
import static io.spine.test.TimeTests.Past.minutesAgo;
import static io.spine.time.Durations2.minutes;
import static io.spine.time.Time.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class CommandSchedulingShould extends AbstractCommandBusTestSuite {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandSchedulingShould.class);

    public CommandSchedulingShould() {
        super(true);
    }

    @Test
    public void store_scheduled_command_and_return_OK() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        verify(commandStore).store(cmd, SCHEDULED);
        checkResult(cmd);
    }

    @Test
    public void schedule_command_if_delay_is_set() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        verify(scheduler).schedule(cmd);
    }

    @Test
    public void not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler());

        final Command command = createProject();
        commandBus.post(command, observer);

        verify(scheduler, never()).schedule(createProject());
        checkResult(command);
    }

    @Test
    public void reschedule_commands_from_storage() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations2.fromMinutes(5);
        final Duration newDelayExpected = Durations2.fromMinutes(2); // = 5 - 3
        final List<Command> commandsPrimary = newArrayList(createProject(),
                                                           addTask(),
                                                           startProject());
        storeAsScheduled(commandsPrimary, delayPrimary, schedulingTime);

        commandBus.rescheduleCommands();

        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(commandsPrimary.size())).schedule(commandCaptor.capture());
        final List<Command> commandsRescheduled = commandCaptor.getAllValues();
        for (Command cmd : commandsRescheduled) {
            final long actualDelay = getDelaySeconds(cmd);
            Tests.assertSecondsEqual(newDelayExpected.getSeconds(), actualDelay, /*maxDiffSec=*/1);
        }
    }

    @Test
    public void reschedule_commands_from_storage_in_parallel_on_build_if_thread_spawning_allowed() {
        final String mainThreadName = Thread.currentThread().getName();
        final StringBuilder threadNameUponScheduling = new StringBuilder(0);
        final CountDownLatch latch = new CountDownLatch(1);
        final CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling, latch);
        storeSingleCommandForRescheduling();

        // Create CommandBus specific for this test.
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
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
        final CountDownLatch latch = new CountDownLatch(1);
        final CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling, latch);
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

        verify(spy).doPost(eq(CommandEnvelope.of(command)));
    }

    @Test(expected = IllegalStateException.class)
    public void reject_previously_scheduled_command_if_no_endpoint_found() {
        Command command = storeSingleCommandForRescheduling();
        commandBus.postPreviouslyScheduled(command);
    }

    @Test
    public void update_schedule_options() {
        final Command cmd = requestFactory.command()
                                          .create(Wrapper.forString(newUuid()));
        final Timestamp schedulingTime = getCurrentTime();
        final Duration delay = Durations2.minutes(5);

        final Command cmdUpdated = setSchedule(cmd, delay, schedulingTime);
        final CommandContext.Schedule schedule = cmdUpdated.getContext()
                                                           .getSchedule();

        assertEquals(delay, schedule.getDelay());
        assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                               .getSchedulingTime());
    }

    @Test
    public void update_scheduling_time() {
        final Command cmd = requestFactory.command()
                                          .create(Wrapper.forString(newUuid()));
        final Timestamp schedulingTime = getCurrentTime();

        final Command cmdUpdated = CommandScheduler.setSchedulingTime(cmd, schedulingTime);

        assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                               .getSchedulingTime());
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
     * @param latch the instance of the {@code CountDownLatch} to await the execution finishing
     * @return newly created instance
     */
    @SuppressWarnings("MethodMayBeStatic") // see Javadoc.
    private CommandScheduler threadAwareScheduler(final StringBuilder targetThreadName,
                                                  final CountDownLatch latch) {
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

    private static long getDelaySeconds(Command cmd) {
        final long delaySec = cmd
                .getContext()
                .getSchedule()
                .getDelay()
                .getSeconds();
        return delaySec;
    }
}
