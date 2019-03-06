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

import static io.spine.base.Time.getCurrentTime;

/**
 * The aggregate representing the lifecycle of a command.
 *
 * <p>All the commands in the system (except the commands in the {@code System} bounded context)
 * have an associated {@code CommandLifecycle}.
 */
@SuppressWarnings({
        "OverlyCoupledClass"  /* OK for an aggregate class. */,
        "PMD.MissingStaticMethodInNonInstantiatableClass" /* Instantiated via reflection. */
})
final class CommandLifecycleAggregate
        extends Aggregate<CommandId, CommandLifecycle, CommandLifecycleVBuilder> {

    private CommandLifecycleAggregate(CommandId id) {
        super(id);
    }

    @Assign
    CommandScheduled handle(ScheduleCommand command) {
        return CommandScheduled.newBuilder()
                               .setId(command.getId())
                               .setSchedule(command.getSchedule())
                               .build();
    }

    @Assign
    TargetAssignedToCommand handle(AssignTargetToCommand event) {
        return TargetAssignedToCommand.newBuilder()
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
        CommandTimeline status = CommandTimeline
                .newBuilder()
                .setWhenReceived(getCurrentTime())
                .build();
        builder().setId(event.getId())
                 .setCommand(event.getPayload())
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
        CommandTimeline status = statusBuilder()
                .setWhenAcknowledged(getCurrentTime())
                .build();
        builder().setId(event.getId())
                 .setStatus(status);
    }

    @Apply(allowImport = true)
    private void on(CommandScheduled event) {
        Command updatedCommand = updateSchedule(event.getSchedule());
        CommandTimeline status = statusBuilder()
                .setWhenScheduled(getCurrentTime())
                .build();
        builder().setId(event.getId())
                 .setCommand(updatedCommand)
                 .setStatus(status);
    }

    /**
     * Imports the event {@link CommandDispatched}.
     *
     * <p>The event is generated when the command is passed to a dispatcher after acknowledgement.
     */
    @Apply(allowImport = true)
    private void on(@SuppressWarnings("unused") CommandDispatched event) {
        CommandTimeline status = statusBuilder()
                .setWhenDispatched(getCurrentTime())
                .build();
        builder().setId(event.getId())
                 .setStatus(status);
    }

    /**
     * Imports the event {@link TargetAssignedToCommand}.
     *
     * <p>The event is generated when the command target is determined.
     */
    @Apply(allowImport = true)
    private void on(TargetAssignedToCommand event) {
        CommandTarget target = event.getTarget();
        CommandLifecycleVBuilder builder = builder();
        CommandTimeline status =
                builder.getStatus()
                       .toBuilder()
                       .setWhenTargetAssigned(getCurrentTime())
                       .build();
        builder.setId(event.getId())
               .setStatus(status)
               .setTarget(target);
    }

    /**
     * Imports the event {@link CommandHandled}.
     *
     * <p>The event is generated after a command is successfully handled.
     */
    @Apply(allowImport = true)
    private void on(@SuppressWarnings("unused") CommandHandled event) {
        setStatus(Responses.statusOk());
    }

    /**
     * Imports the event {@link CommandErrored}.
     *
     * <p>The event is generated if the command caused a runtime error during handling.
     */
    @Apply(allowImport = true)
    private void on(CommandErrored event) {
        Status status = Status
                .newBuilder()
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
        Status status = Status
                .newBuilder()
                .setRejection(event.getRejectionEvent())
                .build();
        setStatus(status);
    }

    @Apply(allowImport = true)
    private void on(CommandTransformed event) {
        Substituted.Builder substituted = Substituted
                .newBuilder()
                .setCommand(event.getId());
        CommandTimeline.Builder newStatus =
                state().getStatus()
                       .toBuilder()
                       .setSubstituted(substituted);
        builder().setStatus(newStatus.build());
    }

    @Apply(allowImport = true)
    private void on(CommandSplit event) {
        Substituted.Builder substituted = Substituted
                .newBuilder()
                .setSequence(Sequence.newBuilder()
                                     .addAllItem(event.getProducedList()));
        CommandTimeline.Builder newStatus =
                state().getStatus()
                       .toBuilder()
                       .setSubstituted(substituted);
        builder().setStatus(newStatus.build());
    }

    private Command updateSchedule(Schedule schedule) {
        Command command = builder().getCommand();
        CommandContext updatedContext =
                command.getContext()
                       .toBuilder()
                       .setSchedule(schedule)
                       .build();
        Command updatedCommand =
                command.toBuilder()
                       .setContext(updatedContext)
                       .build();
        return updatedCommand;
    }

    private CommandTimeline.Builder statusBuilder() {
        return builder().getStatus()
                        .toBuilder();
    }

    private void setStatus(Status status) {
        CommandTimeline commandStatus = statusBuilder()
                .setWhenHandled(getCurrentTime())
                .setHowHandled(status)
                .build();
        builder().setStatus(commandStatus);
    }
}
