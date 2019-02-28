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

package io.spine.server.enrich.given;

import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.server.enrich.Enricher;
import io.spine.server.enrich.EnricherBuilder;
import io.spine.test.event.ProjectId;
import io.spine.testing.core.given.GivenUserId;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;

public class GivenEnricher {

    private GivenEnricher() {
    }

    /** Creates a new enricher with all required enrichment functions set. */
    public static Enricher newEventEnricher() {
        EnricherBuilder builder = Enricher
                .newBuilder();
        return builder.build();
    }

    public static class GetProjectName implements BiFunction<ProjectId, EventContext, String> {
        @Override
        public @Nullable String apply(@Nullable ProjectId id, EventContext context) {
            checkNotNull(id);
            String name = "prj_" + id.getId();
            return name;
        }
    }

    public static class GetProjectOwnerId implements BiFunction<ProjectId, EventContext, UserId> {
        @Override
        public @Nullable UserId apply(@Nullable ProjectId id, EventContext context) {
            checkNotNull(id);
            return GivenUserId.of("po_" + id.getId());
        }
    }
}
