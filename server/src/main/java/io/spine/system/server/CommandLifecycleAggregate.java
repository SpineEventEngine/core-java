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
import io.spine.core.CommandId;
import io.spine.core.EventContext;
import io.spine.core.Responses;
import io.spine.core.Status;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;

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
public final class CommandLifecycleAggregate
        extends Aggregate<CommandId, CommandLifecycle, CommandLifecycleVBuilder> {

    private CommandLifecycleAggregate(CommandId id) {
        super(id);
    }

    @Assign
    CommandReceived handle(ReceiveCommand command) {
        return CommandReceived.newBuilder()
                              .setId(command.getId())
                              .setPayload(command.getPayload())
                              .build();
    }

    @Assign
    CommandAcknowledged handle(AcknowledgeCommand command) {
        return CommandAcknowledged.newBuilder()
                                  .setId(command.getId())
                                  .build();
    }

    @Assign
    CommandDispatched handle(DispatchCommand command) {
        return CommandDispatched.newBuilder()
                                .setId(command.getId())
                                .setReceiver(command.getReceiver())
                                .build();
    }

    @Assign
    CommandHandled handle(MarkCommandAsHandled command) {
        return CommandHandled.newBuilder()
                             .setId(command.getId())
                             .build();
    }

    @Assign
    CommandErrored handle(MarkCommandAsErrored command) {
        return CommandErrored.newBuilder()
                             .setId(command.getId())
                             .setError(command.getError())
                             .build();
    }

    @Assign
    CommandRejected handle(MarkCommandAsRejected command) {
        return CommandRejected.newBuilder()
                              .setId(command.getId())
                              .setRejection(command.getRejection())
                              .build();
    }

    @Apply
    private void on(CommandReceived event, EventContext context) {
        CommandStatus status = CommandStatus
                .newBuilder()
                .setWhenReceived(context.getTimestamp())
                .build();
        getBuilder().setId(event.getId())
                    .setCommand(event.getPayload())
                    .setStatus(status);
    }

    @Apply
    private void on(CommandAcknowledged event, EventContext context) {
        CommandStatus status = getBuilder().getStatus()
                                    .toBuilder()
                                    .setWhenAcknowledged(context.getTimestamp())
                                    .build();
        getBuilder().setStatus(status);
    }

    @Apply
    private void on(CommandDispatched event, EventContext context) {
        CommandStatus status = getBuilder().getStatus()
                                    .toBuilder()
                                    .setWhenDispatched(context.getTimestamp())
                                    .build();
        getBuilder().setStatus(status)
                    .setReceiver(event.getReceiver());
    }

    @Apply
    private void on(CommandHandled event, EventContext context) {
        setProcessingStatus(Responses.statusOk(), context.getTimestamp());
    }

    @Apply
    private void on(CommandErrored event, EventContext context) {
        Status status = Status
                .newBuilder()
                .setError(event.getError())
                .build();
        setProcessingStatus(status, context.getTimestamp());
    }

    @Apply
    private void on(CommandRejected event, EventContext context) {
        Status status = Status
                .newBuilder()
                .setRejection(event.getRejection())
                .build();
        setProcessingStatus(status, context.getTimestamp());
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
