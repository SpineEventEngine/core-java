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

package io.spine.server.commandbus;

import io.spine.base.CommandMessage;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.MemoizingWriteSide;
import io.spine.system.server.WriteSideFunction;
import io.spine.system.server.event.CommandReceived;
import io.spine.test.commands.CmdCreateProject;
import io.spine.testing.client.TestActorRequestFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("CommandReceivedTap should")
class CommandReceivedTapTest {

    private MemoizingWriteSide writeSide;
    private CommandReceivedTap filter;

    @BeforeEach
    void setUp() {
        initSingleTenant();
    }

    private void initSingleTenant() {
        writeSide = MemoizingWriteSide.singleTenant();
        filter = new CommandReceivedTap(systemFn());
    }

    private void initMultitenant() {
        writeSide = MemoizingWriteSide.multitenant();
        filter = new CommandReceivedTap(systemFn());
    }

    private WriteSideFunction systemFn() {
        return WriteSideFunction.delegatingTo(writeSide);
    }

    @Test
    @DisplayName("post MarkCommandAsReceived on command")
    void postSystemCommand() {
        Command command = command(commandMessage(), null);
        postAndCheck(command);
    }

    @Test
    @DisplayName("post MarkCommandAsReceived to specific tenant")
    void postIfMultitenant() {
        initMultitenant();

        TenantId expectedTenant = tenantId();
        Command command = command(commandMessage(), expectedTenant);
        postAndCheck(command);

        TenantId actualTenant = writeSide.lastSeenEvent()
                                         .tenant();
        assertThat(actualTenant)
                .isEqualTo(expectedTenant);
    }

    private void postAndCheck(Command command) {
        CommandEnvelope envelope = CommandEnvelope.of(command);

        Optional<?> ack = filter.accept(envelope);
        assertFalse(ack.isPresent());

        CommandReceived systemEvent = (CommandReceived) writeSide.lastSeenEvent()
                                                                 .message();
        assertThat(systemEvent.getId())
                .isEqualTo(envelope.id());
    }

    private static TenantId tenantId() {
        TenantId tenant = TenantId
                .newBuilder()
                .setValue(CommandReceivedTapTest.class.getSimpleName())
                .build();
        return tenant;
    }

    private static Command command(CommandMessage message, @Nullable TenantId tenantId) {
        TestActorRequestFactory requestFactory =
                tenantId == null
                ? new TestActorRequestFactory(CommandReceivedTapTest.class)
                : new TestActorRequestFactory(CommandReceivedTapTest.class, tenantId);
        Command command = requestFactory.createCommand(message);
        return command;
    }

    private static CommandMessage commandMessage() {
        return CmdCreateProject
                .newBuilder()
                .setId(newUuid())
                .build();
    }
}
