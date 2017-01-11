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

import org.spine3.base.Command;
import org.spine3.base.CommandId;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.setSchedulingTime;
import static org.spine3.protobuf.Timestamps.getCurrentTime;

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
     * Schedules a command and delivers it to the target according to the scheduling options set to a context.
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
        commandBus.doPost(command);
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
}
