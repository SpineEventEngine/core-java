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

package io.spine.server.event.funnel;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.ImportBus;
import io.spine.server.integration.IntegrationBus;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.noOpObserver;

/**
 * Part of the fluent API for manually posting event into a Bounded Context.
 *
 * <p>This step configures the destination bus of the posted events.
 */
public final class AimEvents {

    private static final TenantId defaultTenant = null;
    private static final StreamObserver<Ack> defaultObserver = noOpObserver();

    private final BoundedContext context;
    private final UserId actor;

    AimEvents(BoundedContext context, UserId actor) {
        this.context = checkNotNull(context);
        this.actor = checkNotNull(actor);
    }

    /**
     * Specifies that the events should be posted to an Aggregate via the {@link ImportBus}.
     *
     * <p>If the Aggregate imports the events successfully, they are posted into the Event Bus of
     * the target context, just like any other event applied to an Aggregate.
     *
     * @return the next step in the fluent API for posting events
     */
    public EventFunnel toAggregate() {
        ImportBus bus = context.importBus();
        return new ImportFunnel(defaultTenant, bus, defaultObserver, actor);
    }

    /**
     * Specifies that the events should be posted in the {@link IntegrationBus} and broadcast to
     * external dispatchers.
     *
     * @return the next step in the fluent API for posting events
     */
    public EventFunnel broadcast() {
        IntegrationBus bus = context.integrationBus();
        return new IntegrationFunnel(defaultTenant, bus, defaultObserver, actor);
    }
}
