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

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.spine3.base.CommandContext.Schedule;

/**
 * The command scheduler implementation which uses basic Java task scheduling features.
 *
 * <p><b>NOTE:</b> please use <a href="https://github.com/SpineEventEngine/gae-java">another implementation</a>
 * in applications running under the Google App Engine.
 *
 * @see ScheduledExecutorService
 * @author Alexander Litus
 */
public class ExecutorCommandScheduler extends CommandScheduler {

    private static final int MIN_THREAD_POOL_SIZE = 5;

    private static final Set<CommandId> SCHEDULED_COMMAND_IDS = newHashSet();

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(MIN_THREAD_POOL_SIZE);

    @Override
    protected void doSchedule(final Command command) {
        if (isScheduledAlready(command)) {
            return;
        }
        final long delaySec = getDelaySeconds(command);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                post(command);
            }
        }, delaySec, SECONDS);
        SCHEDULED_COMMAND_IDS.add(command.getContext().getCommandId());
    }

    private static boolean isScheduledAlready(Command command) {
        final CommandId id = command.getContext().getCommandId();
        final boolean isScheduledAlready = SCHEDULED_COMMAND_IDS.contains(id);
        return isScheduledAlready;
    }

    private static long getDelaySeconds(Command command) {
        final Schedule schedule = command.getContext().getSchedule();
        final long delaySec = schedule.getDelay().getSeconds();
        return delaySec;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        executorService.shutdown();
    }
}
