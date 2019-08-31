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
import io.spine.base.EventMessage;
import io.spine.core.Ack;
import io.spine.core.TenantId;

/**
 * A funnel which posts events from an external source into a Bounded Context.
 *
 * <p>This if a part of the fluent API for manually posting events.
 *
 * @see io.spine.server.BoundedContext#postEvents()
 */
public interface EventFunnel {

    /**
     * Specifies an observer for the event acknowledgement.
     *
     * <p>Multiple invocations of this method override the observer.
     *
     * @return a new {@code EventFunnel} with the given observer
     */
    EventFunnel with(StreamObserver<Ack> resultObserver);

    /**
     * Specifies the tenant for which the events are posted.
     *
     * <p>Multiple invocations of this method override the tenant value.
     *
     * @return a new {@code EventFunnel} with the given observer
     */
    EventFunnel forTenant(TenantId tenantId);

    /**
     * Posts the given events into the target Bounded Context.
     *
     * @param events one or more events to post
     */
    void post(EventMessage... events);
}
