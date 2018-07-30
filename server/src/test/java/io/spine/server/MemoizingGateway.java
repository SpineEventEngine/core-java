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

package io.spine.server;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.TenantId;
import io.spine.system.server.SystemGateway;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A {@link SystemGateway} which memoizes the posted system commands.
 */
public final class MemoizingGateway implements SystemGateway {

    private final List<Message> commands = newLinkedList();
    private @Nullable TenantId tenantId;

    @Override
    public void postCommand(Message systemCommand, @Nullable TenantId tenantId) {
        commands.add(systemCommand);
        this.tenantId = tenantId;
    }

    /**
     * Obtains the single posted system command.
     *
     * <p>Fails if the were no commands posted or if there were more then one commands.
     *
     * @return the single posted command message
     */
    @CanIgnoreReturnValue
    public Message oneCommand() {
        assertEquals(1, commands.size());
        return commands.get(0);
    }

    /**
     * Obtains the single {@link TenantId} which the {@link #oneCommand() single command} was posted
     * for.
     *
     * <p>Fails if the were no commands posted or if there were more then one commands. Also fails
     * if the command was posted for the default tenant.
     *
     * @return the single tenant ID
     */
    public TenantId oneTenant() {
        oneCommand();
        checkNotNull(tenantId);
        return tenantId;
    }
}
