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

import com.google.protobuf.Message;
import io.spine.core.CommandId;
import io.spine.core.EventId;

/**
 * A gateway for sending messages into a system bounded context.
 *
 * @author Dmytro Dashenkov
 */
public interface SystemGateway {

    /**
     * Posts a system command.
     *
     * <p>If the associated bounded context is
     * {@linkplain io.spine.server.BoundedContext#isMultitenant() multitenant}, the command is
     * posted for the {@linkplain io.spine.server.tenant.TenantAwareOperation current tenant}.
     *
     * @param systemCommand command message
     */
    void postCommand(Message systemCommand);

    boolean hasHandled(EntityHistoryId entity, CommandId commandId);

    boolean hasHandled(EntityHistoryId entity, EventId eventId);

    /**
     * Creates new instance of the gateway which serves the passed System Bounded Context.
     */
    static SystemGateway newInstance(SystemBoundedContext system) {
        return new DefaultSystemGateway(system);
    }
}
