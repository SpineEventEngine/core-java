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

import io.spine.server.entity.Repository;
import io.spine.system.server.CommandLifecycleRepository;
import io.spine.system.server.EntityHistoryRepository;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.system.server.SystemGateway;

import java.util.stream.Stream;

/**
 * An implementation of {@link BoundedContext} used for the System domain.
 *
 * <p>Orchestrates the system entities, such as
 * {@link io.spine.system.server.EntityHistoryAggregate EntityHistory} and
 * {@link io.spine.system.server.CommandLifecycleAggregate CommandLifecycle}.
 *
 * <p>Users should not access a System bounded context directly. See {@link SystemGateway} for
 * the front-facing API of the System bounded context.
 *
 * @author Dmytro Dashenkov
 * @see SystemGateway
 * @see BoundedContext
 * @see DomainBoundedContext
 */
final class SystemBoundedContext extends BoundedContext {

    private SystemBoundedContext(Builder builder) {
        super(builder);
    }

    static SystemBoundedContext newInstance(Builder builder) {
        SystemBoundedContext result = new SystemBoundedContext(builder);
        result.init();
        return result;
    }

    private void init() {
        Stream.<Repository<?, ?>>of(
                new EntityHistoryRepository(),
                new CommandLifecycleRepository()
        ).forEach(this::register);
    }

    @Override
    public SystemGateway getSystemGateway() {
        return NoOpSystemGateway.INSTANCE;
    }
}
