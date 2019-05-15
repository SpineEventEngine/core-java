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

package io.spine.server.aggregate.given.aggregate;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectPaused;
import io.spine.test.aggregate.event.AggTaskStarted;

import static io.spine.server.route.EventRoute.withId;

/**
 * Test environment repository for {@linkplain io.spine.server.aggregate.IdempotencyGuardTest
 * IdempotencyGuard tests}.
 */
public class IgTestAggregateRepository
        extends AbstractAggregateTestRepository<ProjectId, IgTestAggregate> {

    @OverridingMethodsMustInvokeSuper
    @Override
    public void init() {
        super.init();
        eventRouting().route(AggTaskStarted.class,
                             (message, context) -> withId(message.getProjectId()))
                      .route(AggProjectPaused.class,
                             (message, context) -> withId(message.getProjectId()));
    }
}
