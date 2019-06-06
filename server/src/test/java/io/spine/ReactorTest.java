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

package io.spine;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.EventBus;
import io.spine.server.event.React;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;
import io.spine.system.server.ConstraintViolated;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.spine.server.route.EventRoute.withId;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReactorTest {

    @Test
    void test() {
        SingleTenantBlackBoxContext context = BlackBoxBoundedContext.singleTenant();
        context.with(new PRJR())
               .withEventDispatchers(new Reactor(context.eventBus()));
        PrjProjectCreated event = PrjProjectCreated.newBuilder()
                                                   .setProjectId(ProjectId.newBuilder()
                                                                          .setId("id"))
                                                   .setName("name")
                                                   .vBuild();
        context.receivesEvent(event);
        context.assertEvents()
               .withType(PrjTaskAdded.class)
               .hasSize(1);
    }

    public static class Reactor extends AbstractEventReactor {

        private Reactor(EventBus eventBus) {
            super(eventBus);
        }

        @React(external = true)
        PrjTaskAdded on(ConstraintViolated e) {
            return PrjTaskAdded.newBuilder()
                               .setProjectId(ProjectId.newBuilder().setId("nnnn").build())
                               .vBuild();
        }
    }

    public static class PRJR extends ProjectionRepository<ProjectId, PRJ, ProjectTaskNames> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<ProjectId> routing) {
            super.setupEventRouting(routing);
            routing.route(PrjProjectCreated.class, (e, c) -> withId(e.getProjectId()));
        }
    }

    public static class PRJ extends Projection<ProjectId, ProjectTaskNames, ProjectTaskNames.Builder> {

        @Subscribe
        void on(PrjProjectCreated e) {
            throw new ValidationException(ImmutableList.of(ConstraintViolation.getDefaultInstance()));
        }
    }
}
