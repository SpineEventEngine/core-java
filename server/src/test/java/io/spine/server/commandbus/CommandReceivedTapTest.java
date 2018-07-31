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

import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.server.MemoizingGateway;
import io.spine.system.server.MarkCommandAsReceived;
import io.spine.testing.client.TestActorRequestFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.base.Time.getCurrentTime;
import static io.spine.testing.client.TestActorRequestFactory.newInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("CommandReceivedTap should")
class CommandReceivedTapTest {

    private MemoizingGateway gateway;
    private CommandReceivedTap filter;

    @BeforeEach
    void setUp() {
        initSingleTenant();
    }

    private void initSingleTenant() {
        gateway = MemoizingGateway.singleTenant();
        filter = new CommandReceivedTap(gateway);
    }

    private void initMultitenant() {
        gateway = MemoizingGateway.multitenant();
        filter = new CommandReceivedTap(gateway);
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

        TenantId actualTenant = gateway.receivedTenant();
        assertEquals(expectedTenant, actualTenant);
    }

    private void postAndCheck(Command command) {
        CommandEnvelope envelope = CommandEnvelope.of(command);

        Optional<?> ack = filter.accept(envelope);
        assertFalse(ack.isPresent());

        MarkCommandAsReceived systemCommand = (MarkCommandAsReceived) gateway.receivedCommand();
        assertEquals(envelope.getId(), systemCommand.getId());
    }

    private static TenantId tenantId() {
        TenantId tenant = TenantId
                .newBuilder()
                .setValue(CommandReceivedTapTest.class.getSimpleName())
                .build();
        return tenant;
    }

    private static Command command(Message message, @Nullable TenantId tenantId) {
        TestActorRequestFactory requestFactory =
                tenantId == null
                ? newInstance(CommandReceivedTapTest.class)
                : newInstance(CommandReceivedTapTest.class, tenantId);
        Command command = requestFactory.createCommand(message);
        return command;
    }

    private static Message commandMessage() {
        return getCurrentTime();
    }
}
