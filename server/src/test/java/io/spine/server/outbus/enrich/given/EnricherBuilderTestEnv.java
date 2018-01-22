/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.outbus.enrich.given;

import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.core.CommandContext;
import io.spine.core.EventId;
import io.spine.core.UserId;
import io.spine.core.given.GivenUserId;
import io.spine.people.PersonName;
import io.spine.server.event.EventEnricher;
import io.spine.server.outbus.enrich.Enricher;
import io.spine.test.event.ProjectId;
import io.spine.time.ZoneId;
import io.spine.time.ZoneOffset;

import javax.annotation.Nullable;

/**
 * @author Alexander Yevsyukov
 */
public class EnricherBuilderTestEnv {

    /** Prevent instantiation of this utility class. */
    private EnricherBuilderTestEnv() {}

    public static class Enrichment {

        /** Prevent instantiation of this utility class. */
        private Enrichment() {}

        /** Creates a new enricher with all required enrichment functions set. */
        public static Enricher newEnricher() {
            final EventEnricher.Builder builder = EventEnricher.newBuilder();
            builder.add(ProjectId.class, String.class, new GetProjectName())
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

        public static class GetProjectName implements Function<ProjectId, String> {
            @Nullable
            @Override
            public String apply(@Nullable ProjectId id) {
                if (id == null) {
                    return null;
                }
                final String name = "P-" + id.getId();
                return name;
            }
        }

        public static class GetProjectOwnerId implements Function<ProjectId, UserId> {
            @Nullable
            @Override
            public UserId apply(@Nullable ProjectId id) {
                if (id == null) {
                    return null;
                }
                return GivenUserId.of("PO-" + id.getId());
            }
        }

        private static final Function<EventId, String> EVENT_ID_TO_STRING =
                new Function<EventId, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable EventId input) {
                        return input == null ? "" : input.getValue();
                    }
                };

        private static final Function<Timestamp, String> TIMESTAMP_TO_STRING =
                new Function<Timestamp, String>() {
                    @Override
                    public String apply(@Nullable Timestamp input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<CommandContext, String> CMD_CONTEXT_TO_STRING =
                new Function<CommandContext, String>() {
                    @Override
                    public String apply(@Nullable CommandContext input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<Any, String> ANY_TO_STRING =
                new Function<Any, String>() {
                    @Override
                    public String apply(@Nullable Any input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<Integer, String> VERSION_TO_STRING =
                new Function<Integer, String>() {
                    @Override
                    public String apply(@Nullable Integer input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<String, ZoneOffset> STRING_TO_ZONE_OFFSET =
                new Function<String, ZoneOffset>() {
                    @Nullable
                    @Override
                    public ZoneOffset apply(@Nullable String input) {
                        return input == null
                                ? ZoneOffset.getDefaultInstance()
                                : ZoneOffset.newBuilder()
                                            .setId(ZoneId.newBuilder()
                                                         .setValue(input))
                                            .build();
                    }
                };

        private static final Function<String, PersonName> STRING_TO_PERSON_NAME =
                new Function<String, PersonName>() {
                    @Nullable
                    @Override
                    public PersonName apply(@Nullable String input) {
                        return input == null
                                ? PersonName.getDefaultInstance()
                                : PersonName.newBuilder().setFamilyName(input).build();
                    }
                };

        private static final Function<String, Integer> STRING_TO_INT =
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(@Nullable String input) {
                        return 0;
                    }
                };
    }
}
