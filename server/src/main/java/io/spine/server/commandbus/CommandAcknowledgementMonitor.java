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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.CommandId;
import io.spine.core.Status;
import io.spine.core.TenantId;
import io.spine.system.server.AcknowledgeCommand;
import io.spine.system.server.MarkCommandAsErrored;
import io.spine.system.server.SystemGateway;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A {@link StreamObserver} for {@link io.spine.core.Command Command}
 * {@linkplain Ack acknowledgement}.
 *
 * <p>Posts a system command whenever a command is acknowledged or errored.
 *
 * @author Dmytro Dashenkov
 */
final class CommandAcknowledgementMonitor implements StreamObserver<Ack> {

    private final StreamObserver<Ack> delegate;
    private final TenantId tenantId;

    private final SystemGateway gateway;

    private CommandAcknowledgementMonitor(Builder builder) {
        this.delegate = builder.delegate;
        this.tenantId = builder.tenantId;
        this.gateway = builder.systemGateway;
    }

    @Override
    public void onNext(Ack value) {
        delegate.onNext(value);
        postSystemCommand(value);
    }

    @Override
    public void onError(Throwable t) {
        delegate.onError(t);
    }

    @Override
    public void onCompleted() {
        delegate.onCompleted();
    }

    private void postSystemCommand(Ack ack) {
        Status status = ack.getStatus();
        CommandId commandId = commandIdFrom(ack);
        Message systemCommand = systemCommandFor(status, commandId);
        gateway.postCommand(systemCommand, tenantId);
    }

    private static CommandId commandIdFrom(Ack ack) {
        Any messageId = ack.getMessageId();
        return Identifier.unpack(messageId);
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // Default values.
    private static Message systemCommandFor(Status status, CommandId commandId) {
        switch (status.getStatusCase()) {
            case OK:
                return AcknowledgeCommand.newBuilder()
                                         .setId(commandId)
                                         .build();
            case ERROR:
                return MarkCommandAsErrored.newBuilder()
                                           .setId(commandId)
                                           .setError(status.getError())
                                           .build();
            case REJECTION:
            default:
                throw newIllegalArgumentException("Invalid status %s.", status.getStatusCase());
        }
    }

    /**
     * Creates a new instance of {@code Builder} for {@code CommandAcknowledgementMonitor} instances.
     *
     * @return new instance of {@code Builder}
     */
    static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code CommandAcknowledgementMonitor} instances.
     */
    static final class Builder {

        private StreamObserver<Ack> delegate;
        private TenantId tenantId;
        private SystemGateway systemGateway;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        Builder setDelegate(StreamObserver<Ack> delegate) {
            this.delegate = delegate;
            return this;
        }

        Builder setTenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        Builder setSystemGateway(SystemGateway systemGateway) {
            this.systemGateway = systemGateway;
            return this;
        }

        /**
         * Creates a new instance of {@code CommandAcknowledgementMonitor}.
         *
         * @return new instance of {@code CommandAcknowledgementMonitor}
         */
        CommandAcknowledgementMonitor build() {
            return new CommandAcknowledgementMonitor(this);
        }
    }

}
