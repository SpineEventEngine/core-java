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
import io.spine.annotation.Internal;
import io.spine.core.TenantId;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A gateway for sending messages into a system bounded context.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public interface SystemGateway {

    /**
     * Posts a system command.
     *
     * <p>In a multitenant environment, the command is posted for the current tenant.
     *
     * @param systemCommand command message
     */
    default void postCommand(Message systemCommand) {
        postCommand(systemCommand, null);
    }

    /**
     * Posts a system command for the given tenant.
     *
     * <p>If the {@code tenantId} is {@code null} or
     * {@linkplain io.spine.validate.Validate#isDefault(Message) default}, posts the command for
     * the {@linkplain io.spine.server.tenant.TenantFunction current tenant} in multitenant
     * environment or the default tenant in a single-tenant environment.
     *
     * @param systemCommand the system command to post
     * @param tenantId      the ID of the tenant to post the command for
     */
    void postCommand(Message systemCommand, @Nullable TenantId tenantId);
}
