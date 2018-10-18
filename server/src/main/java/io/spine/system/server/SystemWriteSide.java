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

import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The write side of a system bounded context.
 *
 * <p>A domain context posts messages to its system counterpart via a {@code SystemWriteSide}.
 */
@Internal
public interface SystemWriteSide {

    /**
     * Posts a system command.
     *
     * <p>If the associated bounded context is
     * {@linkplain io.spine.server.BoundedContext#isMultitenant() multitenant}, the command is
     * posted for the {@linkplain io.spine.server.tenant.TenantAwareOperation current tenant}.
     *
     * @param systemCommand command message
     */
    void postCommand(CommandMessage systemCommand);

    /**
     * Posts a system event.
     *
     * <p>If the associated bounded context is
     * {@linkplain io.spine.server.BoundedContext#isMultitenant() multitenant}, the event is
     * posted for the {@linkplain io.spine.server.tenant.TenantAwareOperation current tenant}.
     *
     * @param systemEvent event message
     */
    void postEvent(EventMessage systemEvent);

    /**
     * Creates new instance of the {@code SystemWriteSide} which serves the passed system context.
     */
    static SystemWriteSide newInstance(SystemContext system) {
        checkNotNull(system);
        return new DefaultSystemWriteSide(system);
    }
}
