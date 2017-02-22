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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.time.Interval;

import java.util.Iterator;

import static com.google.protobuf.util.Timestamps.add;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.base.Commands.setSchedule;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.protobuf.Timestamps2.isLaterThan;
import static org.spine3.server.command.error.CommandExpiredException.commandExpiredError;
import static org.spine3.time.Intervals.between;
import static org.spine3.time.Intervals.toDuration;

/**
 * Helper class for rescheduling commands.
 *
 * @author Alexander Yevsyukov
 */
class Rescheduler {

    private final CommandBus commandBus;

    Rescheduler(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    void rescheduleCommands() {
        final Runnable reschedulingAction = new Runnable() {
            @Override
            public void run() {
                doRescheduleCommands();
            }
        };

        if (commandBus.isThreadSpawnAllowed()) {
            final Thread thread = new Thread(reschedulingAction, "CommandBus-rescheduleCommands");
            thread.start();
        } else {
            reschedulingAction.run();
        }
    }

    @VisibleForTesting
    void doRescheduleCommands() {
        final Iterator<Command> commands = commandBus.commandStore().iterator(SCHEDULED);
        while (commands.hasNext()) {
            final Command command = commands.next();
            reschedule(command);
        }
    }

    private void reschedule(Command command) {
        final Timestamp now = getCurrentTime();
        final Timestamp timeToPost = getTimeToPost(command);
        if (isLaterThan(now, /*than*/ timeToPost)) {
            onScheduledCommandExpired(command);
        } else {
            final Interval interval = between(now, timeToPost);
            final Duration newDelay = toDuration(interval);
            final Command commandUpdated = setSchedule(command, newDelay, now);
            commandBus.scheduler().schedule(commandUpdated);
        }
    }

    private static Timestamp getTimeToPost(Command command) {
        final CommandContext.Schedule schedule = command.getContext()
                                                        .getSchedule();
        final Timestamp timeToPost = add(schedule.getSchedulingTime(), schedule.getDelay());
        return timeToPost;
    }

    private void onScheduledCommandExpired(Command command) {
        // We cannot post this command because there is no handler/dispatcher registered yet.
        // Also, posting it can be undesirable.
        final Message msg = getMessage(command);
        final CommandId id = getId(command);
        commandBus.problemLog().errorExpiredCommand(msg, id);
        commandBus.getCommandStatusService().setToError(id, commandExpiredError(msg));
    }
}
