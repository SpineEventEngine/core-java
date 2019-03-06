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

package io.spine.server.aggregate.given;

import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.Version;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Snapshot;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectVBuilder;
import io.spine.test.storage.event.StgProjectCreated;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.core.Versions.increment;
import static io.spine.core.Versions.zero;

public final class ReadOperationTestEnv {

    private static Version version = zero();

    /**
     * Prevents the utility class instantiation.
     */
    private ReadOperationTestEnv() {
    }

    public static List<Event> events(int count) {
        return Stream.generate(ReadOperationTestEnv::newEvent)
                     .limit(count)
                     .collect(toImmutableList());
    }

    private static Event newEvent() {
        EventId id = EventId
                .newBuilder()
                .setValue(newUuid())
                .build();
        version = increment(version);
        EventContext context = EventContext
                .newBuilder()
                .setTimestamp(currentTime())
                .setVersion(version)
                .build();
        return Event
                .newBuilder()
                .setId(id)
                .setContext(context)
                .setMessage(pack(StgProjectCreated.getDefaultInstance()))
                .build();
    }

    public static Snapshot snapshot() {
        version = increment(version);
        return Snapshot
                .newBuilder()
                .setTimestamp(currentTime())
                .setVersion(version)
                .build();
    }

    public static final class TestAggregate extends Aggregate<String, Project, ProjectVBuilder> {
        private TestAggregate(String id) {
            super(id);
        }
    }
}
