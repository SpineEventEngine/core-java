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
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.protobuf.util.Timestamps.checkValid;
import static org.spine3.base.Commands.getId;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * Schedules commands delivering them to the target according to the scheduling options.
 *
 * @author Alexander Litus
 */
public abstract class CommandScheduler {

    private static final Set<CommandId> scheduledCommandIds = newHashSet();

    private boolean isActive = true;

    private CommandBus commandBus;

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
        markAsScheduled(commandUpdated);
    }

    /**
     * Obtains {@code CommandBus} associated with this scheduler or {@code null}
     * if the scheduler is not linked to a {@code CommandBus}.
     */
    @Nullable
    @CheckReturnValue
    protected CommandBus getCommandBus() {
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

    /**
     * Delivers a scheduled command to a target.
     *
     * @param command a command to deliver
     */
    protected void post(Command command) {
        commandBus.postPreviouslyScheduled(command);
    }

    private static boolean isScheduledAlready(Command command) {
        final CommandId id = getId(command);
        final boolean isScheduledAlready = scheduledCommandIds.contains(id);
        return isScheduledAlready;
    }

    private static void markAsScheduled(Command command) {
        final CommandId id = getId(command);
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

    /** Sets a command bus used to post scheduled commands. */
    void setCommandBus(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    /**
     * Sets a new scheduling time to {@link CommandContext.Schedule}.
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
     * Updates {@link CommandContext.Schedule}.
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
                                                               .setSchedulingTime(schedulingTime)
                                                               .build();
        final CommandContext contextUpdated = context.toBuilder()
                                                     .setSchedule(scheduleUpdated)
                                                     .build();
        final Command result = command.toBuilder()
                                      .setContext(contextUpdated)
                                      .build();
        return result;
    }
}
