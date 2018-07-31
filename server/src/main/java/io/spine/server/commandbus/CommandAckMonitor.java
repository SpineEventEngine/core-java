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
import io.spine.system.server.MarkCommandAsAcknowledged;
import io.spine.system.server.MarkCommandAsErrored;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A {@link StreamObserver} for {@link io.spine.core.Command Command}
 * {@linkplain Ack acknowledgement}.
 *
 * <p>Posts a system command whenever a command is acknowledged or errored.
 *
 * <p>{@code CommandAckMonitor} is designed to wrap instances of {@link StreamObserver}.
 * All the calls to {@link StreamObserver} methods on an instance of {@code CommandAckMonitor}
 * invoke respective methods on a {@code delegate} instance.
 *
 * @author Dmytro Dashenkov
 */
final class CommandAckMonitor implements StreamObserver<Ack> {

    private final StreamObserver<Ack> delegate;

    private final SystemGateway gateway;

    private CommandAckMonitor(Builder builder) {
        this.delegate = builder.delegate;
        this.gateway = TenantAwareSystemGateway
                .create()
                .atopOf(builder.systemGateway)
                .withTenant(builder.tenantId)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts either {@link MarkCommandAsAcknowledged} or {@link MarkCommandAsErrored} system command
     * depending on the value of the given {@code Ack}.
     *
     * @param value
     */
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
        gateway.postCommand(systemCommand);
    }

    private static CommandId commandIdFrom(Ack ack) {
        Any messageId = ack.getMessageId();
        return Identifier.unpack(messageId);
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // Default values.
    private static Message systemCommandFor(Status status, CommandId commandId) {
        switch (status.getStatusCase()) {
            case OK:
                return MarkCommandAsAcknowledged.newBuilder()
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
     * Creates a new instance of {@code Builder} for {@code CommandAckMonitor} instances.
     *
     * @return new instance of {@code Builder}
     */
    static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code CommandAckMonitor} instances.
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

        /**
         * @param delegate the {@link StreamObserver} to delegate calls to
         */
        Builder setDelegate(StreamObserver<Ack> delegate) {
            this.delegate = checkNotNull(delegate);
            return this;
        }

        /**
         * @param tenantId the ID of a tenant which owns the observed commands
         */
        Builder setTenantId(TenantId tenantId) {
            this.tenantId = checkNotNull(tenantId);
            return this;
        }

        /**
         * @param systemGateway the {@link SystemGateway} to post system commands into
         */
        Builder setSystemGateway(SystemGateway systemGateway) {
            this.systemGateway = checkNotNull(systemGateway);
            return this;
        }

        /**
         * Creates a new instance of {@code CommandAckMonitor}.
         *
         * @return new instance of {@code CommandAckMonitor}
         */
        CommandAckMonitor build() {
            checkNotNull(delegate);
            checkNotNull(tenantId);
            checkNotNull(systemGateway);

            return new CommandAckMonitor(this);
        }
    }
}
