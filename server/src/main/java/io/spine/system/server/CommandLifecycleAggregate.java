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

package io.spine.system.server;

import com.google.protobuf.Timestamp;
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
 *
 * <p>Emits events:
 * <ul>
 *     <li>{@link CommandReceived} - when a command is received by the
 *         {@link io.spine.server.commandbus.CommandBus CommandBus};
 *     <li>{@link CommandAcknowledged} - when the command passes
 *         the {@linkplain io.spine.server.bus.BusFilter bus filters} successfully;
 *     <li>{@link CommandErrored} - when the command causes an error in
 *         the {@linkplain io.spine.server.bus.BusFilter bus filters};
 *     <li>{@link CommandDispatched} - when the command is passed to a dispatcher after
 *         being acknowledged;
 *     <li>{@link TargetAssignedToCommand} - when the command target is determined;
 *     <li>{@link CommandHandled} - after a successful command handling;
 *     <li>{@link CommandErrored} - if the command caused a runtime error during handling;
 *     <li>{@link CommandRejected} - if the command handler rejected the command.
 * </ul>

 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods"}) // OK for an aggregate class.
public final class CommandLifecycleAggregate
        extends Aggregate<CommandId, CommandLifecycle, CommandLifecycleVBuilder> {

    private CommandLifecycleAggregate(CommandId id) {
        super(id);
    }

    @Assign
    CommandReceived handle(MarkCommandAsReceived command) {
        Timestamp when = getCurrentTime();
        return CommandReceived.newBuilder()
                              .setId(command.getId())
                              .setPayload(command.getPayload())
                              .setWhen(when)
                              .build();
    }

    @Assign
    CommandAcknowledged handle(MarkCommandAsAcknowledged command) {
        Timestamp when = getCurrentTime();
        return CommandAcknowledged.newBuilder()
                                  .setId(command.getId())
                                  .setWhen(when)
                                  .build();
    }

    @Assign
    CommandScheduled handle(ScheduleCommand command) {
        Timestamp when = getCurrentTime();
        return CommandScheduled.newBuilder()
                               .setId(command.getId())
                               .setSchedule(command.getSchedule())
                               .setWhen(when)
                               .build();
    }

    @Assign
    CommandDispatched handle(MarkCommandAsDispatched command) {
        Timestamp when = getCurrentTime();
        return CommandDispatched.newBuilder()
                                .setId(command.getId())
                                .setWhen(when)
                                .build();
    }

    @Assign
    TargetAssignedToCommand on(AssignTargetToCommand event) {
        Timestamp when = getCurrentTime();
        return TargetAssignedToCommand.newBuilder()
                                      .setId(event.getId())
                                      .setTarget(event.getTarget())
                                      .setWhen(when)
                                      .build();
    }

    @Assign
    CommandHandled handle(MarkCommandAsHandled command) {
        Timestamp when = getCurrentTime();
        return CommandHandled.newBuilder()
                             .setId(command.getId())
                             .setWhen(when)
                             .build();
    }

    @Assign
    CommandErrored handle(MarkCommandAsErrored command) {
        Timestamp when = getCurrentTime();
        return CommandErrored.newBuilder()
                             .setId(command.getId())
                             .setError(command.getError())
                             .setWhen(when)
                             .build();
    }

    @Assign
    CommandRejected handle(MarkCommandAsRejected command) {
        Timestamp when = getCurrentTime();
        return CommandRejected.newBuilder()
                              .setId(command.getId())
                              .setRejectionEvent(command.getRejectionEvent())
                              .setWhen(when)
                              .build();
    }

    @Apply
    void on(CommandReceived event) {
        CommandTimeline status = CommandTimeline
                .newBuilder()
                .setWhenReceived(event.getWhen())
                .build();
        getBuilder().setId(event.getId())
                    .setCommand(event.getPayload())
                    .setStatus(status);
    }

    private CommandTimeline.Builder statusBuilder() {
        return getBuilder().getStatus()
                           .toBuilder();
    }

    @Apply
    void on(CommandAcknowledged event) {
        CommandTimeline status = statusBuilder()
                .setWhenAcknowledged(event.getWhen())
                .build();
        getBuilder().setStatus(status);
    }

    @Apply
    void on(CommandScheduled event) {
        Command updatedCommand = updateSchedule(event.getSchedule());
        CommandTimeline status = statusBuilder()
                .setWhenScheduled(event.getWhen())
                .build();
        getBuilder().setCommand(updatedCommand)
                    .setStatus(status);
    }

    @Apply
    void on(CommandDispatched event) {
        CommandTimeline status = statusBuilder()
                .setWhenDispatched(event.getWhen())
                .build();
        getBuilder().setStatus(status);
    }

    @Apply
    void on(TargetAssignedToCommand event) {
        CommandTarget target = event.getTarget();
        CommandTimeline status = getBuilder()
                .getStatus()
                .toBuilder()
                .setWhenTargetAssgined(event.getWhen())
                .build();
        getBuilder()
                .setStatus(status)
                .setTarget(target);
    }

    @Apply
    void on(CommandHandled event) {
        setStatus(Responses.statusOk(), event.getWhen());
    }

    @Apply
    void on(CommandErrored event) {
        Status status = Status
                .newBuilder()
                .setError(event.getError())
                .build();
        setStatus(status, event.getWhen());
    }

    @Apply
    void on(CommandRejected event) {
        Status status = Status
                .newBuilder()
                .setRejection(event.getRejectionEvent())
                .build();
        setStatus(status, event.getWhen());
    }

    @Assign
    CommandTransformed on(MarkTransformed cmd) {
        return CommandTransformed.newBuilder()
                                 .setId(cmd.getId())
                                 .setProduced(cmd.getProduced())
                                 .build();
    }

    @Apply
    void event(CommandTransformed event) {
        Substituted.Builder substituted = Substituted
                .newBuilder()
                .setCommand(event.getId());
        CommandTimeline.Builder newStatus =
                getState().getStatus()
                          .toBuilder()
                          .setSubstitued(substituted);
        getBuilder().setStatus(newStatus.build());
    }

    @Assign
    CommandSplit on(MarkSplit cmd) {
        return CommandSplit.newBuilder()
                           .setId(cmd.getId())
                           .addAllProduced(cmd.getProducedList())
                           .build();
    }

    @Apply
    void event(CommandSplit event) {
        Substituted.Builder substituted = Substituted
                .newBuilder()
                .setSequence(Sequence.newBuilder()
                                     .addAllItem(event.getProducedList()));
        CommandTimeline.Builder newStatus =
                getState().getStatus()
                          .toBuilder()
                          .setSubstitued(substituted);
        getBuilder().setStatus(newStatus.build());
    }

    private Command updateSchedule(Schedule schedule) {
        Command command = getBuilder().getCommand();
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

    private void setStatus(Status status, Timestamp whenProcessed) {
        CommandTimeline commandStatus = statusBuilder()
                .setWhenHandled(whenProcessed)
                .setHowHandled(status)
                .build();
        getBuilder().setStatus(commandStatus);
    }
}
