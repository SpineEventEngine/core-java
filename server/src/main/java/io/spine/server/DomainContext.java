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
import io.spine.system.server.MasterWriteSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.system.server.SystemReadSide;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A bounded context representing a user-specific domain model.
 *
 * <p>The {@link BoundedContext} instances typically seen to the users
 * (i.e. built with a {@link BoundedContextBuilder}) are instances of this class.
 *
 * <p>All the user interactions with the system (such as
 * {@linkplain BoundedContext#register(Repository) repository registration},
 * {@linkplain BoundedContext#getCommandBus() command posting},
 * {@linkplain BoundedContext#findRepository(Class) query processing}, etc.) happen through
 * an instance of this class.
 *
 * <p>Each {@code DomainContext} has an associated
 * {@link io.spine.system.server.SystemContext SystemContext}, which manages the meta information
 * about entities of this Bounded Context.
 *
 * @see io.spine.system.server.SystemContext SystemContext
 */
final class DomainContext extends BoundedContext {

    private final MasterWriteSide systemGateway;
    private final SystemReadSide systemReadSide;

    private DomainContext(BoundedContextBuilder builder,
                          MasterWriteSide gateway,
                          SystemReadSide systemReadSide) {
        super(builder);
        this.systemGateway = gateway;
        this.systemReadSide = systemReadSide;
    }

    static DomainContext newInstance(BoundedContextBuilder builder,
                                     MasterWriteSide gateway,
                                     SystemReadSide bus) {
        checkNotNull(builder);
        checkNotNull(gateway);
        checkNotNull(builder);

        DomainContext result = new DomainContext(builder, gateway, bus);
        return result;
    }

    @Override
    public SystemWriteSide getSystemGateway() {
        return systemGateway;
    }

    @Override
    public SystemReadSide getSystemReadSide() {
        return systemReadSide;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Closes the system context as well.
     *
     * @throws Exception if the system context throws an error on closing
     */
    @Override
    public void close() throws Exception {
        super.close();
        systemGateway.closeSystemContext();
    }
}
