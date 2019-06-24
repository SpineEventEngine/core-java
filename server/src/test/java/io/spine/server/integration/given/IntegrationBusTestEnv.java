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
package io.spine.server.integration.given;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.test.integration.ProjectId;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;
import io.spine.testing.server.TestEventFactory;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static io.spine.testing.server.TestEventFactory.newInstance;

/**
 * Test environment for {@link io.spine.server.integration.IntegrationBusTest}.
 */
public class IntegrationBusTestEnv {

    /** Prevents instantiation of this utility class. */
    private IntegrationBusTestEnv() {
    }

    @CanIgnoreReturnValue
    public static BoundedContext
    contextWithExtEntitySubscribers(TransportFactory transportFactory) {
        BoundedContext boundedContext = contextWithTransport(transportFactory);
        boundedContext.register(new ProjectDetailsRepository());
        boundedContext.register(new ProjectWizardRepository());
        boundedContext.register(new ProjectCountAggregateRepository());
        return boundedContext;
    }

    @CanIgnoreReturnValue
    public static BoundedContext contextWithExternalSubscribers(TransportFactory transportFactory) {
        BoundedContext boundedContext = contextWithTransport(transportFactory);
        AbstractEventSubscriber eventSubscriber = new ProjectEventsSubscriber();
        boundedContext.integrationBus()
                      .register(eventSubscriber);
        boundedContext.eventBus()
                      .register(eventSubscriber);
        boundedContext.register(new ProjectCountAggregateRepository());
        boundedContext.register(new ProjectWizardRepository());
        return boundedContext;
    }

    public static BoundedContext contextWithTransport(TransportFactory transportFactory) {
        BoundedContext result = BoundedContext
                .singleTenant(newUuid())
                .setTransportFactory(transportFactory)
                .build();
        return result;
    }

    public static BoundedContext contextWithProjectCreatedNeeds(TransportFactory factory) {
        BoundedContext result = contextWithTransport(factory);
        result.integrationBus()
              .register(new ProjectEventsSubscriber());
        return result;
    }

    public static BoundedContext contextWithProjectStartedNeeds(TransportFactory factory) {
        BoundedContext result = contextWithTransport(factory);
        result.integrationBus()
              .register(new ProjectStartedExtSubscriber());
        return result;
    }

    public static Event projectCreated() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory = newInstance(pack(projectId), IntegrationBusTestEnv.class);
        return eventFactory.createEvent(
                ItgProjectCreated.newBuilder()
                                 .setProjectId(projectId)
                                 .build()
        );
    }

    public static Event projectStarted() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                newInstance(pack(projectId), IntegrationBusTestEnv.class);
        return eventFactory.createEvent(
                ItgProjectStarted.newBuilder()
                                 .setProjectId(projectId)
                                 .build()
        );
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }
}
