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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * The default command scheduler implementation.
 *
 * @author Alexander Litus
 */
public class DefaultCommandScheduler implements CommandScheduler {

    private static final int MIN_THEAD_POOL_SIZE = 5;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(MIN_THEAD_POOL_SIZE);

    private boolean isShutdown = false;

    private final CommandBus commandBus;

    /**
     * Creates a new instance.
     *
     * @param commandBus a command bus used to send scheduled commands
     */
    public DefaultCommandScheduler(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    public void schedule(Command command) {
        checkState(!isShutdown, "Scheduler is shut down.");
        final long delaySec = getDelaySeconds(command);
        final Command commandUpdated = removeSchedulingOptions(command);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                commandBus.post(commandUpdated);
            }
        }, delaySec, SECONDS);
    }

    private static long getDelaySeconds(Command command) {
        final CommandContext context = command.getContext();
        final long delaySec;
        final Duration delay = context.getDelay();
        if (isNotDefault(delay)) {
            delaySec = delay.getSeconds();
        } else {
            final Timestamp sendingTime = context.getSendingTime();
            final Timestamp now = getCurrentTime();
            delaySec = sendingTime.getSeconds() - now.getSeconds();
        }
        return delaySec;
    }

    private static Command removeSchedulingOptions(Command command) {
        final CommandContext contextUpdated = command.getContext().toBuilder()
                .clearDelay()
                .clearSendingTime()
                .build();
        final Command result = command.toBuilder()
                .setContext(contextUpdated)
                .build();
        return result;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
        isShutdown = true;
    }
}
