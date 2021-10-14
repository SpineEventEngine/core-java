/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.server.integration.given.broker;

import com.google.common.base.Throwables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.DefaultRepository;
import io.spine.server.integration.given.ProjectCommander;
import io.spine.server.integration.given.ProjectCountAggregate;
import io.spine.server.integration.given.ProjectDetails;
import io.spine.server.integration.given.ProjectEventsSubscriber;
import io.spine.server.integration.given.ProjectStartedExtSubscriber;
import io.spine.server.integration.given.ProjectWizard;
import io.spine.test.integration.ProjectId;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxContext;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static io.spine.testing.server.TestEventFactory.newInstance;

/**
 * Test environment for {@link io.spine.server.integration.IntegrationBrokerTest}.
 */
public class IntegrationBrokerTestEnv {

    private static final String testClassName = IntegrationBrokerTestEnv.class.getSimpleName();

    /** Prevents instantiation of this utility class. */
    private IntegrationBrokerTestEnv() {
    }

    @CanIgnoreReturnValue
    public static BoundedContext
    contextWithExternalEntitySubscribers() {
        BoundedContext context = newContext();
        context.internalAccess()
               .register(DefaultRepository.of(ProjectCountAggregate.class))
               .register(DefaultRepository.of(ProjectWizard.class))
               .register(DefaultRepository.of(ProjectDetails.class));
        return context;
    }

    @CanIgnoreReturnValue
    public static BoundedContext contextWithExternalSubscribers() {
        BoundedContext context = newContext();
        context.internalAccess()
               .register(DefaultRepository.of(ProjectCountAggregate.class))
               .register(DefaultRepository.of(ProjectWizard.class))
               .registerEventDispatcher(new ProjectEventsSubscriber())
               .registerCommandDispatcher(new ProjectCommander());
        return context;
    }

    public static BoundedContext newContext() {
        BoundedContext result = BoundedContext
                .singleTenant(newUuid())
                .build();
        return result;
    }

    public static BoundedContext contextWithProjectCreatedNeeds() {
        BoundedContext result = BoundedContext
                .singleTenant(newUuid())
                .addEventDispatcher(new ProjectEventsSubscriber())
                .build();
        return result;
    }

    public static BoundedContext contextWithProjectStartedNeeds() {
        BoundedContext result = BoundedContext
                .singleTenant(newUuid())
                .addEventDispatcher(new ProjectStartedExtSubscriber())
                .build();
        return result;
    }

    public static Event projectCreated() {
        ProjectId projectId = ProjectId.newBuilder()
                                       .setId(Throwables.getStackTraceAsString(
                                               new RuntimeException("Project ID")
                                       ))
                                       .build();

        TestEventFactory eventFactory = newInstance(
                pack(projectId),
                IntegrationBrokerTestEnv.class
        );

        return eventFactory.createEvent(
                ItgProjectCreated.newBuilder()
                                 .setProjectId(projectId)
                                 .build()
        );
    }

    public static Event projectStarted() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                newInstance(pack(projectId), IntegrationBrokerTestEnv.class);
        return eventFactory.createEvent(
                ItgProjectStarted.newBuilder()
                                 .setProjectId(projectId)
                                 .build()
        );
    }

    public static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    // **********************
    // proof of concept #2
    // **********************

    public static BlackBoxContext billingBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("BillingBc-" + testClassName)
                              .add(BillingAggregate.class)
        );
    }

    public static BlackBoxContext subscribedBillingBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("SubscribedBillingBc-" + testClassName)
                              .add(SubscribedBillingAggregate.class)
        );
    }

    public static BlackBoxContext photosBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("PhotosBc-" + testClassName)
                              .add(PhotosAggregate.class)
        );
    }

    public static BlackBoxContext subscribedPhotosBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("SubscribedPhotosBc-" + testClassName)
                              .add(SubscribedPhotosAggregate.class)
        );
    }

    public static BlackBoxContext subscribedWarehouseBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("SubscribedWarehouseBc-" + testClassName)
                              .add(SubscribedWarehouseAggregate.class)
        );
    }

    public static BlackBoxContext subscribedStatisticsBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("SubscribedStatisticsBc-" + testClassName)
                              .add(SubscribedStatisticsAggregate.class)
        );
    }

    public static BlackBoxContext photosBcAndSubscribedBillingBc() {
        return BlackBoxContext.from(
                BoundedContext.singleTenant("photosBcAndSubscribedBillingBc-" + testClassName)
                              .add(PhotosAggregate.class)
                              .add(SubscribedBillingAggregate.class)
        );
    }
}
