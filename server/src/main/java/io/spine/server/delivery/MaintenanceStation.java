/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.delivery.event.ShardProcessingRequested;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * A station which supplies the {@link DeliveryRunInfo} to certain events being dispatched
 * to {@link ShardMaintenanceProcess}.
 */
final class MaintenanceStation extends Station {

    private final DeliveryRunInfo runInfo;
    private final ImmutableList<CatchUp> finalizingJobs;

    /**
     * Creates a station in this delivery run-time information and initializes itself by searching
     * for the matching catch-up jobs.
     */
    MaintenanceStation(DeliveryRunInfo runInfo) {
        super();
        this.runInfo = runInfo;
        this.finalizingJobs = findFinalizingJobs(runInfo);
    }

    /**
     * Updates the {@code ShardProcessingRequested} events by setting
     * the current {@link DeliveryRunInfo} into each, if there is at least one
     * {@code CatchUp} job in {@link CatchUpStatus#FINALIZING FINALIZING} status.
     *
     * <p>The updated messages are put back into the conveyor and later they are delivered
     * via {@link LiveDeliveryStation}.
     *
     * @param conveyor
     *         the conveyor on which the messages are travelling
     * @return empty result, as no messages are ever delivered.
     */
    @Override
    Result process(Conveyor conveyor) {
        if (finalizingJobs.isEmpty()) {
            return emptyResult();
        }
        updateShardProcessingEvents(conveyor);
        return emptyResult();
    }

    private void updateShardProcessingEvents(Conveyor conveyor) {
        for (InboxMessage message : conveyor) {
            if (message.hasEvent()) {
                Event event = message.getEvent();
                EventMessage eventMessage = event.enclosedMessage();
                if (eventMessage instanceof ShardProcessingRequested) {
                    ShardProcessingRequested cast = (ShardProcessingRequested) eventMessage;
                    ShardProcessingRequested updatedSignal = updateWithContext(cast);
                    updateEventMessage(conveyor, message, updatedSignal);
                }
            }
        }
    }

    private static void
    updateEventMessage(Conveyor conveyor, InboxMessage message, EventMessage newValue) {
        Event event = message.getEvent();
        Event modifiedEvent = event.toBuilder()
                                   .setMessage(pack(newValue))
                                   .vBuild();
        InboxMessage modifiedMessage = message.toBuilder()
                                              .setEvent(modifiedEvent)
                                              .vBuild();
        conveyor.update(modifiedMessage);
    }

    private ShardProcessingRequested updateWithContext(ShardProcessingRequested signal) {
        ShardProcessingRequested modifiedSignal = signal.toBuilder()
                                                        .setRunInfo(runInfo)
                                                        .vBuild();
        return modifiedSignal;
    }

    private static ImmutableList<CatchUp> findFinalizingJobs(DeliveryRunInfo runInfo) {
        List<CatchUp> jobs = runInfo.getCatchUpJobList();
        ImmutableList<CatchUp> finalizingJobs =
                jobs.stream()
                    .filter((job) -> job.getStatus() == CatchUpStatus.FINALIZING)
                    .collect(toImmutableList());
        return finalizingJobs;
    }
}
