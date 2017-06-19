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
import io.grpc.stub.StreamObserver;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.CommandId;
import io.spine.envelope.CommandEnvelope;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.protobuf.util.Timestamps.checkValid;
import static io.spine.base.CommandStatus.SCHEDULED;
import static io.spine.base.Commands.isScheduled;
import static io.spine.time.Time.getCurrentTime;

/**
 * Schedules commands delivering them to the target according to the scheduling options.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public abstract class CommandScheduler implements CommandBusFilter {

    private static final Set<CommandId> scheduledCommandIds = newHashSet();

    private boolean isActive = true;

    @Nullable
    private CommandBus commandBus;

    @Nullable
    private Rescheduler rescheduler;

    protected CommandScheduler() {
    }

    /**
     * Assigns the {@code CommandBus} to the scheduler during {@code CommandBus}
     * {@linkplain CommandBus.Builder#build() construction}.
     */
    protected void setCommandBus(CommandBus commandBus) {
        this.commandBus = commandBus;
        this.rescheduler = new Rescheduler(commandBus);
    }

    private Rescheduler rescheduler() {
        checkState(rescheduler != null, "Rescheduler is not initialized");
        return rescheduler;
    }

    @Override
    public boolean accept(CommandEnvelope envelope, StreamObserver<Command> observer) {
        final Command command = envelope.getCommand();
        if (isScheduled(command)) {
            scheduleAndStore(command, observer);
            return false;
        }
        return true;
    }

    private void scheduleAndStore(Command command, StreamObserver<Command> observer) {
        schedule(command);
        commandBus().commandStore()
                    .store(command, SCHEDULED);
        observer.onNext(command);
        observer.onCompleted();
    }

    @Override
    public void onClose(CommandBus commandBus) {
        shutdown();
    }

    /**
     * Schedules a command and delivers it to the target according to the scheduling options.
     *
     * <p>A command with the same ID cannot be scheduled again.
     *
     * @param command a command to deliver later
     * @throws IllegalStateException if the scheduler is shut down
     */
    public void schedule(Command command) {
        checkState(isActive, "Scheduler is shut down.");
        if (isScheduledAlready(command)) {
            return;
        }
        final Command commandUpdated = setSchedulingTime(command, getCurrentTime());
        doSchedule(commandUpdated);
        rememberAsScheduled(commandUpdated);
    }

    /**
     * Obtains {@code CommandBus} associated with this scheduler.
     *
     * @throws IllegalStateException if {@code CommandBus} was not
     * {@linkplain #setCommandBus(CommandBus) set} prior to calling this method
     */
    @CheckReturnValue
    protected CommandBus commandBus() {
        checkState(commandBus != null, "CommandBus is not set");
        return commandBus;
    }

    /**
     * Schedules a command and delivers it to the target according to
     * the scheduling options set to a context.
     *
     * @param command a command to deliver later
     * @see #post(Command)
     */
    protected abstract void doSchedule(Command command);

    void rescheduleCommands() {
        rescheduler().rescheduleCommands();
    }

    /**
     * Delivers a scheduled command to a target.
     *
     * @param command a command to deliver
     */
    protected void post(Command command) {
        commandBus().postPreviouslyScheduled(command);
    }

    private static boolean isScheduledAlready(Command command) {
        final CommandId id = command.getId();
        final boolean isScheduledAlready = scheduledCommandIds.contains(id);
        return isScheduledAlready;
    }

    private static void rememberAsScheduled(Command command) {
        final CommandId id = command.getId();
        scheduledCommandIds.add(id);
    }

    /**
     * Initiates an orderly shutdown in which previously scheduled commands will be delivered later,
     * but no new commands will be accepted.
     *
     * <p>Invocation has no effect if the scheduler is already shut down.
     */
    public void shutdown() {
        isActive = false;
    }

    /**
     * Sets a new scheduling time in the {@linkplain CommandContext.Schedule context}
     * of the passed command.
     *
     * @param command        a command to update
     * @param schedulingTime the time when the command was scheduled by the {@code CommandScheduler}
     * @return an updated command
     */
    static Command setSchedulingTime(Command command, Timestamp schedulingTime) {
        checkNotNull(command);
        checkNotNull(schedulingTime);

        final Duration delay = command.getContext()
                                      .getSchedule()
                                      .getDelay();
        final Command result = setSchedule(command, delay, schedulingTime);
        return result;
    }

    /**
     * Updates {@linkplain CommandContext.Schedule command schedule}.
     *
     * @param command        a command to update
     * @param delay          a {@linkplain CommandContext.Schedule#getDelay() delay} to set
     * @param schedulingTime the time when the command was scheduled by the {@code CommandScheduler}
     * @return an updated command
     */
    static Command setSchedule(Command command, Duration delay, Timestamp schedulingTime) {
        checkNotNull(command);
        checkNotNull(delay);
        checkNotNull(schedulingTime);
        checkValid(schedulingTime);

        final CommandContext context = command.getContext();
        final CommandContext.Schedule scheduleUpdated = context.getSchedule()
                                                               .toBuilder()
                                                               .setDelay(delay)
                                                               .build();
        final CommandContext contextUpdated = context.toBuilder()
                                                     .setSchedule(scheduleUpdated)
                                                     .build();

        final Command.SystemProperties sysProps = command.getSystemProperties()
                                                         .toBuilder()
                                                         .setSchedulingTime(schedulingTime)
                                                         .build();
        final Command result = command.toBuilder()
                                      .setContext(contextUpdated)
                                      .setSystemProperties(sysProps)
                                      .build();
        return result;
    }
}
