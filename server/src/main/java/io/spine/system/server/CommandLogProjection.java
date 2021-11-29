/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.system.server;

import io.spine.core.Command;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.CommandId;
import io.spine.core.Responses;
import io.spine.core.Status;
import io.spine.core.Subscribe;
import io.spine.core.Where;
import io.spine.server.projection.Projection;
import io.spine.system.server.Substituted.Sequence;
import io.spine.system.server.event.CommandAcknowledged;
import io.spine.system.server.event.CommandDispatched;
import io.spine.system.server.event.CommandErrored;
import io.spine.system.server.event.CommandHandled;
import io.spine.system.server.event.CommandReceived;
import io.spine.system.server.event.CommandRejected;
import io.spine.system.server.event.CommandScheduled;
import io.spine.system.server.event.CommandSplit;
import io.spine.system.server.event.CommandTransformed;
import io.spine.system.server.event.TargetAssignedToCommand;

import static io.spine.base.Time.currentTime;

/**
 * The aggregate representing the lifecycle of a command.
 *
 * <p>All the commands in the system (except the commands in the {@code System} bounded context)
 * have an associated {@code CommandLog}.
 */
@SuppressWarnings("OverlyCoupledClass") // because of the handled commands
final class CommandLogProjection
        extends Projection<CommandId, CommandLog, CommandLog.Builder> {

    @Subscribe
    void on(CommandReceived event) {
        timeline().setWhenReceived(currentTime());
        builder().setCommand(event.getPayload());
    }

    @Subscribe
    void on(@SuppressWarnings("unused") CommandAcknowledged event) {
        timeline().setWhenAcknowledged(currentTime());
    }

    @Subscribe
    void on(CommandScheduled event) {
        var updatedCommand = updateSchedule(event.getSchedule());
        timeline().setWhenScheduled(currentTime());
        builder().setCommand(updatedCommand);
    }

    @Subscribe
    void on(@SuppressWarnings("unused") CommandDispatched event) {
        timeline().setWhenDispatched(currentTime());
    }

    @Subscribe
    void on(TargetAssignedToCommand event) {
        var target = event.getTarget();
        timeline().setWhenTargetAssigned(currentTime());
        builder().setTarget(target);
    }

    @Subscribe
    void on(@SuppressWarnings("unused") CommandHandled event) {
        setStatus(Responses.statusOk());
    }

    @Subscribe
    void on(@Where(field = "handled_signal.id.type_url",
                   equals = "type.spine.io/spine.core.Command")
            HandlerFailedUnexpectedly event) {
        var status = Status.newBuilder()
                .setError(event.getError())
                .build();
        setStatus(status);
    }

    @Subscribe
    void on(CommandErrored event) {
        var status = Status.newBuilder()
                .setError(event.getError())
                .build();
        setStatus(status);
    }

    /**
     * Imports the event {@link CommandRejected}.
     *
     * <p>The event is generated if the command handler rejected the command.
     */
    @Subscribe
    void on(CommandRejected event) {
        var status = Status.newBuilder()
                .setRejection(event.getRejectionEvent())
                .build();
        setStatus(status);
    }

    @Subscribe
    void on(CommandTransformed event) {
        var substituted = Substituted.newBuilder()
                .setCommand(event.getId())
                .build();
        timeline().setSubstituted(substituted);
    }

    @Subscribe
    void on(CommandSplit event) {
        var sequence = Sequence.newBuilder()
                .addAllItem(event.getProducedList())
                .build();
        var substituted = Substituted.newBuilder()
                .setSequence(sequence)
                .build();
        timeline().setSubstituted(substituted);

    }

    private Command updateSchedule(Schedule schedule) {
        var command = builder().getCommand();
        var updatedContext = command.context()
                .toBuilder()
                .setSchedule(schedule)
                .build();
        var updatedCommand = command.toBuilder()
                .setContext(updatedContext)
                .build();
        return updatedCommand;
    }

    private CommandTimeline.Builder timeline() {
        return builder().getTimelineBuilder();
    }

    private void setStatus(Status status) {
        timeline()
                .setWhenHandled(currentTime())
                .setHowHandled(status);
    }
}
