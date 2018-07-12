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

import com.google.common.annotations.VisibleForTesting;
import io.spine.system.server.DefaultSystemGateway;
import io.spine.system.server.SystemGateway;

/**
 * The default implementation of a {@link BoundedContext}.
 *
 * <p>All the user interactions with the system (such as repository registration, command posting,
 * query processing, etc.) happen through an instance of this class.
 *
 * @author Dmytro Dashenkov
 * @see SystemBoundedContext
 */
final class DomainBoundedContext extends BoundedContext {

    private final SystemBoundedContext system;
    private final SystemGateway systemGateway;

    private DomainBoundedContext(Builder builder, SystemBoundedContext system) {
        super(builder);
        this.system = system;
        this.systemGateway = new DefaultSystemGateway(system);
    }

    static DomainBoundedContext newInstance(Builder builder, SystemBoundedContext system) {
        DomainBoundedContext result = new DomainBoundedContext(builder, system);
        result.init();
        return result;
    }

    private void init() {
        getStand().onCreated(this);
    }

    @VisibleForTesting
    BoundedContext system() {
        return system;
    }

    @Override
    public SystemGateway getSystemGateway() {
        return systemGateway;
    }
}
