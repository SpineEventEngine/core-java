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
import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.CommandId;
import io.spine.core.Responses;
import io.spine.core.Status;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;

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
 *     <li>{@link CommandHandled} - after a successful command handling;
 *     <li>{@link CommandErrored} - if the command caused a runtime error during handling;
 *     <li>{@link CommandRejected} - if the command handler rejected the command.
 * </ul>

 * @author Dmytro Dashenkov
 */
@SuppressWarnings("OverlyCoupledClass") // OK for an aggregate class.
@Internal
public final class CommandLifecycleAggregate
        extends Aggregate<CommandId, CommandLifecycle, CommandLifecycleVBuilder> {

    private CommandLifecycleAggregate(CommandId id) {
        super(id);
    }

    @Assign
    CommandReceived handle(ReceiveCommand command) {
        Timestamp when = getCurrentTime();
        return CommandReceived.newBuilder()
                              .setId(command.getId())
                              .setPayload(command.getPayload())
                              .setWhen(when)
                              .build();
    }

    @Assign
    CommandAcknowledged handle(AcknowledgeCommand command) {
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
    CommandDispatched handle(DispatchCommand command) {
        Timestamp when = getCurrentTime();
        return CommandDispatched.newBuilder()
                                .setId(command.getId())
                                .setWhen(when)
                                .build();
    }

    @Assign
    CommandHandled handle(MarkCommandAsHandled command) {
        Timestamp when = getCurrentTime();
        return CommandHandled.newBuilder()
                             .setId(command.getId())
                             .setReceiver(command.getReceiver())
                             .setWhen(when)
                             .build();
    }

    @Assign
    CommandErrored handle(MarkCommandAsErrored command) {
        Timestamp when = getCurrentTime();
        return CommandErrored.newBuilder()
                             .setId(command.getId())
                             .setError(command.getError())
                             .setReceiver(command.getReceiver())
                             .setWhen(when)
                             .build();
    }

    @Assign
    CommandRejected handle(MarkCommandAsRejected command) {
        Timestamp when = getCurrentTime();
        return CommandRejected.newBuilder()
                              .setId(command.getId())
                              .setRejection(command.getRejection())
                              .setReceiver(command.getReceiver())
                              .setWhen(when)
                              .build();
    }

    @Apply
    private void on(CommandReceived event) {
        CommandStatus status = CommandStatus
                .newBuilder()
                .setWhenReceived(event.getWhen())
                .build();
        getBuilder().setId(event.getId())
                    .setCommand(event.getPayload())
                    .setStatus(status);
    }

    @Apply
    private void on(CommandAcknowledged event) {
        CommandStatus status = getBuilder()
                .getStatus()
                .toBuilder()
                .setWhenAcknowledged(event.getWhen())
                .build();
        getBuilder().setStatus(status);
    }

    @Apply
    private void on(CommandScheduled event) {
        Command updatedCommand = updateSchedule(event.getSchedule());
        CommandStatus status = getBuilder()
                .getStatus()
                .toBuilder()
                .setWhenScheduled(event.getWhen())
                .build();
        getBuilder().setCommand(updatedCommand)
                    .setStatus(status);
    }

    @Apply
    private void on(CommandDispatched event) {
        CommandStatus status = getBuilder()
                .getStatus()
                .toBuilder()
                .setWhenDispatched(event.getWhen())
                .build();
        getBuilder().setStatus(status);
    }

    @Apply
    private void on(CommandHandled event) {
        setProcessingStatus(Responses.statusOk(), event.getWhen());
        getBuilder().setReceiver(event.getReceiver());
    }

    @Apply
    private void on(CommandErrored event) {
        Status status = Status
                .newBuilder()
                .setError(event.getError())
                .build();
        setProcessingStatus(status, event.getWhen());
        getBuilder().setReceiver(event.getReceiver());
    }

    @Apply
    private void on(CommandRejected event) {
        Status status = Status
                .newBuilder()
                .setRejection(event.getRejection())
                .build();
        setProcessingStatus(status, event.getWhen());
        getBuilder().setReceiver(event.getReceiver());
    }

    private Command updateSchedule(Schedule schedule) {
        CommandContext updatedContext = getBuilder().getCommand()
                                                    .getContext()
                                                    .toBuilder()
                                                    .setSchedule(schedule)
                                                    .build();
        Command updatedCommand = getBuilder().getCommand()
                                             .toBuilder()
                                             .setContext(updatedContext)
                                             .build();
        return updatedCommand;
    }

    private void setProcessingStatus(Status status, Timestamp whenProcessed) {
        CommandStatus commandStatus = getBuilder().getStatus()
                                                  .toBuilder()
                                                  .setWhenProcessed(whenProcessed)
                                                  .setProcessingStatus(status)
                                                  .build();
        getBuilder().setStatus(commandStatus);
    }
}
