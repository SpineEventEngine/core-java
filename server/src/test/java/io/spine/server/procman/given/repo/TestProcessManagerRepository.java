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
import io.spine.server.entity.rejection.StandardRejection;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.route.EventRouting;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.server.route.EventRoute.withId;

public final class TestProcessManagerRepository
        extends ProcessManagerRepository<ProjectId, TestProcessManager, Project> {

    private @Nullable RuntimeException latestException;

    @Override
    protected void setupEventRouting(EventRouting<ProjectId> routing) {
        super.setupEventRouting(routing);
        routing.route(StandardRejection.class,
                      (event, context) -> withId((ProjectId) event.entityId()));
    }

    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        this.latestException = exception;
        super.onError(event, exception);
    }

    @Override
    public void onError(CommandEnvelope cmd, RuntimeException exception) {
        this.latestException = exception;
        try {
            super.onError(cmd, exception);
        } catch (Exception e) {
            //TODO:2019-06-17:alex.tymchenko: Do we need to let the exceptions out at all?
            // see https://github.com/SpineEventEngine/core-java/issues/1094
            logError("Error dispatching command (class: `%s`, ID: `%s`) to entity with state `%s`.",
                     cmd, exception);

        }
    }

    public @Nullable RuntimeException latestException() {
        return latestException;
    }

    @Override
    public EventFilter eventFilter() {
        return super.eventFilter();
    }
}
