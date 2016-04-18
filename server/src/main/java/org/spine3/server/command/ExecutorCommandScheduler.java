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
import org.spine3.base.Schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

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

    private static final int MIN_THEAD_POOL_SIZE = 5;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(MIN_THEAD_POOL_SIZE);

    @Override
    public void schedule(final Command command) {
        super.schedule(command);
        final long delaySec = getDelaySeconds(command);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                post(command);
            }
        }, delaySec, SECONDS);
    }

    private static long getDelaySeconds(Command command) {
        final Schedule schedule = command.getContext().getSchedule();
        final long delaySec = schedule.getAfter().getSeconds();
        return delaySec;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        executorService.shutdown();
    }
}
