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

package io.spine.server.aggregate.given.repo;

import com.google.common.collect.ImmutableSet;
import io.spine.core.EventContext;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.route.EventRoute;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.rejection.Rejections.AggCannotStartArchivedProject;

import java.util.Set;

public class RejectionReactingRepository
        extends AggregateRepository<ProjectId, RejectionReactingAggregate> {

    public RejectionReactingRepository() {
        super();
        getEventRouting()
                .route(AggCannotStartArchivedProject.class, routeRejection());
    }

    private static EventRoute<ProjectId, AggCannotStartArchivedProject> routeRejection() {
        return new EventRoute<ProjectId, AggCannotStartArchivedProject>() {
            private static final long serialVersionUID = 0L;

            @Override
            public Set<ProjectId> apply(
                    AggCannotStartArchivedProject message,
                    EventContext context) {
                return ImmutableSet.copyOf(message.getChildProjectIdList());
            }
        };
    }
}
