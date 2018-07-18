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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.CommandIndex;
import io.spine.time.Durations2;
import io.spine.time.Timestamps2;

import java.util.Set;

import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.time.Timestamps2.isLaterThan;

/**
 * Helper class for rescheduling commands.
 *
 * @author Alexander Yevsyukov
 */
class Rescheduler {

    private final CommandBus bus;
    private final TenantIndex tenantIndex;
    private final CommandIndex commandIndex;

    private Rescheduler(Builder builder) {
        this.bus = builder.bus;
        this.tenantIndex = builder.tenantIndex;
        this.commandIndex = builder.commandIndex;
    }

    void rescheduleCommands() {
        Runnable reschedulingAction = this::doRescheduleCommands;

        if (bus.isThreadSpawnAllowed()) {
            Thread thread = new Thread(reschedulingAction, "CommandBus-rescheduleCommands");
            thread.start();
        } else {
            reschedulingAction.run();
        }
    }

    private CommandScheduler scheduler() {
        return bus.scheduler();
    }

    private Log log() {
        return bus.problemLog();
    }

    @VisibleForTesting
    private void doRescheduleCommands() {
        Set<TenantId> tenants = tenantIndex.getAll();
        for (TenantId tenantId : tenants) {
            rescheduleForTenant(tenantId);
        }
    }

    private void rescheduleForTenant(TenantId tenantId) {
        TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                doReschedule();
            }
        };
        operation.execute();
    }

    private void doReschedule() {
        commandIndex.scheduledCommands()
                    .forEachRemaining(this::reschedule);
    }

    private void reschedule(Command command) {
        Timestamp now = getCurrentTime();
        Timestamp timeToPost = getTimeToPost(command);
        if (isLaterThan(now, /*than*/ timeToPost)) {
            onScheduledCommandExpired(command);
        } else {
            Duration newDelay = Durations2.of(java.time.Duration.between(
                    Timestamps2.toInstant(now),
                    Timestamps2.toInstant(timeToPost))
            );
            Command updatedCommand = CommandScheduler.setSchedule(command, newDelay, now);
            scheduler().schedule(updatedCommand);
        }
    }

    private static Timestamp getTimeToPost(Command command) {
        CommandContext.Schedule schedule = command.getContext()
                                                  .getSchedule();
        Timestamp timeToPost = add(command.getSystemProperties()
                                          .getSchedulingTime(), schedule.getDelay());
        return timeToPost;
    }

    /**
     * Sets the status of the expired command to error.
     *
     * <p>We cannot post such a command because there is no handler or dispatcher registered yet.
     * Or, posting such a command may be undesirable from the business logic point of view.
     *
     * @param command the expired command
     * @see CommandExpiredException
     */
    private void onScheduledCommandExpired(Command command) {
        CommandEnvelope commandEnvelope = CommandEnvelope.of(command);
        Message msg = commandEnvelope.getMessage();
        CommandId id = commandEnvelope.getId();

        // TODO:2018-07-17:dmytro.dashenkov: Post MarkCommandAsErrored.
        log().errorExpiredCommand(msg, id);
    }

    /**
     * Creates a new instance of {@code Builder} for {@code Rescheduler} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code Rescheduler} instances.
     */
    static final class Builder {

        private CommandBus bus;
        private TenantIndex tenantIndex;
        private CommandIndex commandIndex;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        Builder setBus(CommandBus bus) {
            this.bus = bus;
            return this;
        }

        Builder setTenantIndex(TenantIndex tenantIndex) {
            this.tenantIndex = tenantIndex;
            return this;
        }

        Builder setCommandIndex(CommandIndex commandIndex) {
            this.commandIndex = commandIndex;
            return this;
        }

        /**
         * Creates a new instance of {@code Rescheduler}.
         *
         * @return new instance of {@code Rescheduler}
         */
        Rescheduler build() {
            return new Rescheduler(this);
        }
    }

}
