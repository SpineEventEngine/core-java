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
import io.spine.core.Command;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.spine.core.CommandContext.Schedule;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
    private static final int NANOS_IN_MILLISECOND = 1_000_000;
    private static final int MILLIS_IN_SECOND = 1_000;

    private final ScheduledExecutorService executorService =
            Executors.newScheduledThreadPool(MIN_THREAD_POOL_SIZE);

    public ExecutorCommandScheduler() {
        super();
    }

    @Override
    protected void doSchedule(Command command) {
        long delaySec = getDelayMilliseconds(command);
        executorService.schedule(() -> post(command), delaySec, MILLISECONDS);
    }

    private static long getDelayMilliseconds(Command command) {
        Schedule schedule = command.getContext()
                                   .getSchedule();
        Duration delay = schedule.getDelay();
        long delaySec = delay.getSeconds();
        long delayMillisFraction = delay.getNanos() / NANOS_IN_MILLISECOND;

        /**
         * Maximum value of {@link Duration#getSeconds()} is
         * <a href="https://github.com/google/protobuf/blob/master/src/google/protobuf/duration.proto"+315,576,000,000.</a>.
         *
         * {@link Long.MAX_VALUE} is +9,223,372,036,854,775,807. That's why it is safe to multiply
         * {@code delaySec * MILLIS_IN_SECOND}.
         */
        long absoluteMillis = delaySec * MILLIS_IN_SECOND + delayMillisFraction;
        return absoluteMillis;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        executorService.shutdown();
    }
}
