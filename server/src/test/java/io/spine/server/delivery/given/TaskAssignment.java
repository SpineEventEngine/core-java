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

package io.spine.server.delivery.given;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.server.event.React;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.test.delivery.DTaskAssigned;
import io.spine.test.delivery.DTaskAssignment;
import io.spine.test.delivery.DTaskCreated;
import io.spine.testing.core.given.GivenUserId;

public class TaskAssignment
        extends ProcessManager<String, DTaskAssignment, DTaskAssignment.Builder> {

    @React
    DTaskAssigned on(DTaskCreated event) {
        String rawId = event.getId();
        builder().setId(rawId);
        return DTaskAssigned.newBuilder()
                            .setId(rawId)
                            .setAssignee(GivenUserId.generated())
                            .vBuild();
    }

    public static class Repository
            extends ProcessManagerRepository<String, TaskAssignment, DTaskAssignment> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<String> routing) {
            super.setupEventRouting(routing);
            routing.route(DTaskCreated.class, routeTaskCreated());
        }

        @SuppressWarnings("UnnecessaryLambda")
        private static EventRoute<String, DTaskCreated> routeTaskCreated() {
            return (EventRoute<String, DTaskCreated>)
                    (message, context) -> ImmutableSet.of(message.getId());
        }
    }
}
