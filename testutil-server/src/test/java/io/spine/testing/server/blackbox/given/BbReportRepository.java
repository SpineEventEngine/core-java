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

package io.spine.testing.server.blackbox.given;

import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.route.EventRoute;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BbReportId;
import io.spine.testing.server.blackbox.event.BbTaskAdded;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toSet;

/**
 * A Report repository routing the {@link BbTaskAdded Task Added} events to all reports containing
 * corresponding project.
 */
public class BbReportRepository extends AggregateRepository<BbReportId, BbReportAggregate> {

    private final List<BbReportAggregate> aggregates = newArrayList();

    public BbReportRepository() {
        eventRouting().route(BbTaskAdded.class, (EventRoute<BbReportId, BbTaskAdded>)
                (event, context) -> getReportsContainingProject(event.getProjectId()));
    }

    @Override
    public BbReportAggregate create(BbReportId id) {
        BbReportAggregate aggregate = super.create(id);
        aggregates.add(aggregate);
        return aggregate;
    }

    private Set<BbReportId> getReportsContainingProject(BbProjectId projectId) {
        return aggregates
                .stream()
                .filter(report -> reportContainsProject(report, projectId))
                .map(AbstractEntity::id)
                .collect(toSet());
    }

    private static boolean reportContainsProject(BbReportAggregate report, BbProjectId projectId) {
        return report.state()
                     .getProjectIdsList()
                     .contains(projectId);
    }
}
