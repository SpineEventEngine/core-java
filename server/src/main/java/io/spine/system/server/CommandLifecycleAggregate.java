/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.CommandId;
import io.spine.core.Responses;
import io.spine.core.Status;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.system.server.Substituted.Sequence;
import io.spine.system.server.command.AssignTargetToCommand;
import io.spine.system.server.command.ScheduleCommand;
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
 * have an associated {@code CommandLifecycle}.
 */
@SuppressWarnings("OverlyCoupledClass") // because of the handled commands
final class CommandLifecycleAggregate
        extends Aggregate<CommandId, CommandLifecycle, CommandLifecycleVBuilder> {

    @Assign
    CommandScheduled handle(ScheduleCommand command) {
        return CommandScheduled.vBuilder()
                               .setId(command.getId())
                               .setSchedule(command.getSchedule())
                               .build();
    }

    @Assign
    TargetAssignedToCommand handle(AssignTargetToCommand event) {
        return TargetAssignedToCommand.vBuilder()
                                      .setId(event.getId())
                                      .setTarget(event.getTarget())
                                      .build();
    }

    /**
     * Imports the event {@link CommandReceived}.
     *
     * <p>The event is generated when a command is received by the
     * {@link io.spine.server.commandbus.CommandBus CommandBus}.
     */
    @Apply(allowImport = true)
    private void on(CommandReceived event) {
        ensureId();
        CommandTimeline status = CommandTimeline
                .vBuilder()
                .setWhenReceived(currentTime())
                .build();
        builder().setCommand(event.getPayload())
                 .setStatus(status);
    }

    /**
     * Imports the event {@link CommandAcknowledged}.
     *
     * <p>The event is generated when the command passes
     * {@linkplain io.spine.server.bus.BusFilter bus filters} successfully;
     */
    @Apply(allowImport = true)
    private void on(@SuppressWarnings("unused") CommandAcknowledged event) {
        ensureId();
        CommandTimeline status = statusBuilder()
                .setWhenAcknowledged(currentTime())
                .build();
        builder().setStatus(status);
    }

    @Apply(allowImport = true)
    private void on(CommandScheduled event) {
        ensureId();
        Command updatedCommand = updateSchedule(event.getSchedule());
        CommandTimeline status = statusBuilder()
                .setWhenScheduled(currentTime())
                .build();
        builder().setCommand(updatedCommand)
                 .setStatus(status);
    }

    /**
     * Imports the event {@link CommandDispatched}.
     *
     * <p>The event is generated when the command is passed to a dispatcher after acknowledgement.
     */
    @Apply(allowImport = true)
    private void on(@SuppressWarnings("unused") CommandDispatched event) {
        ensureId();
        CommandTimeline status = statusBuilder()
                .setWhenDispatched(currentTime())
                .build();
        builder().setStatus(status);
    }

    /**
     * Imports the event {@link TargetAssignedToCommand}.
     *
     * <p>The event is generated when the command target is determined.
     */
    @Apply(allowImport = true)
    private void on(TargetAssignedToCommand event) {
        ensureId();
        CommandTarget target = event.getTarget();
        CommandLifecycleVBuilder builder = builder();
        CommandTimeline status =
                builder.getStatus()
                       .toVBuilder()
                       .setWhenTargetAssigned(currentTime())
                       .build();
        builder.setStatus(status)
               .setTarget(target);
    }

    /**
     * Imports the event {@link CommandHandled}.
     *
     * <p>The event is generated after a command is successfully handled.
     */
    @Apply(allowImport = true)
    private void on(@SuppressWarnings("unused") CommandHandled event) {
        ensureId();
        setStatus(Responses.statusOk());
    }

    /**
     * Imports the event {@link CommandErrored}.
     *
     * <p>The event is generated if the command caused a runtime error during handling.
     */
    @Apply(allowImport = true)
    private void on(CommandErrored event) {
        ensureId();
        Status status = Status
                .vBuilder()
                .setError(event.getError())
                .build();
        setStatus(status);
    }

    /**
     * Imports the event {@link CommandRejected}.
     *
     * <p>The event is generated if the command handler rejected the command.
     */
    @Apply(allowImport = true)
    private void on(CommandRejected event) {
        ensureId();
        Status status = Status
                .vBuilder()
                .setRejection(event.getRejectionEvent())
                .build();
        setStatus(status);
    }

    @Apply(allowImport = true)
    private void on(CommandTransformed event) {
        ensureId();
        Substituted substituted = Substituted
                .vBuilder()
                .setCommand(event.getId())
                .build();
        CommandTimeline newStatus =
                state().getStatus()
                       .toVBuilder()
                       .setSubstituted(substituted)
                       .build();
        builder().setStatus(newStatus);
    }

    @Apply(allowImport = true)
    private void on(CommandSplit event) {
        ensureId();
        Sequence sequence = Sequence
                .vBuilder()
                .addAllItem(event.getProducedList())
                .build();
        Substituted substituted = Substituted
                .vBuilder()
                .setSequence(sequence)
                .build();
        CommandTimeline newStatus =
                state().getStatus()
                       .toVBuilder()
                       .setSubstituted(substituted)
                       .build();
        builder().setStatus(newStatus);
    }

    private Command updateSchedule(Schedule schedule) {
        Command command = builder().getCommand();
        CommandContext updatedContext =
                command.getContext()
                       .toVBuilder()
                       .setSchedule(schedule)
                       .build();
        Command updatedCommand =
                command.toVBuilder()
                       .setContext(updatedContext)
                       .build();
        return updatedCommand;
    }

    private CommandTimelineVBuilder statusBuilder() {
        return builder().getStatus()
                        .toVBuilder();
    }

    private void setStatus(Status status) {
        CommandTimeline commandStatus = statusBuilder()
                .setWhenHandled(currentTime())
                .setHowHandled(status)
                .build();
        builder().setStatus(commandStatus);
    }

    private void ensureId() {
        builder().setId(id());
    }
}
