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

import io.spine.server.BoundedContext;
import io.spine.testing.server.blackbox.BlackBoxContext;

/**
 * Test environment for {@link io.spine.server.integration.IntegrationBrokerTest}.
 */
public class IntegrationBrokerTestEnv {

    private static final String testClassName = IntegrationBrokerTestEnv.class.getSimpleName();

    private IntegrationBrokerTestEnv() {
    }

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
