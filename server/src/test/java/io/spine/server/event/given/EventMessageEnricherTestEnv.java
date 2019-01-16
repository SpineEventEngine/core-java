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

package io.spine.server.event.given;

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.UserId;
import io.spine.people.PersonName;
import io.spine.server.event.enrich.Builder;
import io.spine.server.event.enrich.Enricher;
import io.spine.test.event.ProjectId;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.ZoneOffset;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.BiFunction;

public class EventMessageEnricherTestEnv {

    /** Prevent instantiation of this utility class. */
    private EventMessageEnricherTestEnv() {
    }

    public static class Enrichment {

        private static final BiFunction<EventId, EventContext, String> EVENT_ID_TO_STRING =
                (input, context) -> input == null ? "" : input.getValue();
        private static final BiFunction<Timestamp, EventContext, String> TIMESTAMP_TO_STRING =
                (input, context) -> input == null ? "" : input.toString();
        private static final BiFunction<CommandContext, EventContext, String> CMD_CONTEXT_TO_STRING =
                (input, context) -> input == null ? "" : input.toString();
        private static final BiFunction<Any, EventContext, String> ANY_TO_STRING =
                (input, context) -> input == null ? "" : input.toString();
        private static final BiFunction<Integer, EventContext, String> VERSION_TO_STRING =
                (input, context) -> input == null ? "" : input.toString();
        private static final BiFunction<String, EventContext, ZoneOffset> STRING_TO_ZONE_OFFSET =
                new StringToZoneOffset();
        private static final BiFunction<String, EventContext, PersonName> STRING_TO_PERSON_NAME =
                new StringToPersonName();
        private static final BiFunction<String, EventContext, Integer> STRING_TO_INT =
                (input, context) ->  Integer.valueOf(input);

        private Enrichment() {
        }

        /** Creates a new enricher with all required enrichment functions set. */
        public static Enricher newEventEnricher() {
            Builder builder = Enricher
                    .newBuilder()
                    .add(ProjectId.class, String.class, new GetProjectName())
                    .add(ProjectId.class, UserId.class, new GetProjectOwnerId())
                    .add(EventId.class, String.class, EVENT_ID_TO_STRING)
                    .add(Timestamp.class, String.class, TIMESTAMP_TO_STRING)
                    .add(CommandContext.class, String.class, CMD_CONTEXT_TO_STRING)
                    .add(Any.class, String.class, ANY_TO_STRING)
                    .add(Integer.class, String.class, VERSION_TO_STRING)
                    .add(String.class, ZoneOffset.class, STRING_TO_ZONE_OFFSET)
                    .add(String.class, PersonName.class, STRING_TO_PERSON_NAME)
                    .add(String.class, Integer.class, STRING_TO_INT);
            return builder.build();
        }

        public static class GetProjectName implements BiFunction<ProjectId, EventContext, String> {
            @Override
            public @Nullable String apply(@Nullable ProjectId id, EventContext context) {
                if (id == null) {
                    return null;
                }
                String name = "pr-" + id.getId();
                return name;
            }
        }

        public static class GetProjectOwnerId implements BiFunction<ProjectId, EventContext, UserId> {
            @Override
            public @Nullable UserId apply(@Nullable ProjectId id, EventContext context) {
                if (id == null) {
                    return null;
                }
                return GivenUserId.of("po-" + id.getId());
            }
        }
    }
}
