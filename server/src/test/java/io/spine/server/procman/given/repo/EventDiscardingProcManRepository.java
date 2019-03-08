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

package io.spine.server.procman.given.repo;

import io.spine.server.entity.EventFilter;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;

import java.util.Optional;

/**
 * A repository managing {@link TestProcessManager}s and discarding all the events from being
 * posted.
 *
 * <p>Note that this behaviour is not expected in production PM repositories, since
 * the "discard all" strategy breaks event PM dispatching. The same is true for
 * the {@link io.spine.server.projection.ProjectionRepository ProjectionRepository}-s.
 */
public final class EventDiscardingProcManRepository
        extends ProcessManagerRepository<ProjectId, TestProcessManager, Project> {

    private static final EventFilter eventFilter = anyEvent -> Optional.empty();

    @Override
    protected EventFilter eventFilter() {
        return eventFilter;
    }
}
