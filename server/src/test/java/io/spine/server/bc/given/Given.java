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

package io.spine.server.bc.given;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.event.EventDispatcher;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.test.bc.ProjectId;
import io.spine.test.bc.command.BcCreateProject;
import io.spine.test.bc.event.BcProjectCreated;
import io.spine.test.bc.event.BcProjectStarted;
import io.spine.test.bc.event.BcTaskAdded;

import java.util.Optional;
import java.util.Set;

public class Given {

    private Given() {
    }

    public static class EventMessage {

        private EventMessage() {
        }

        public static BcProjectCreated projectCreated(ProjectId id) {
            return BcProjectCreated.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }

        public static BcTaskAdded taskAdded(ProjectId id) {
            return BcTaskAdded.newBuilder()
                              .setProjectId(id)
                              .build();
        }

        public static BcProjectStarted projectStarted(ProjectId id) {
            return BcProjectStarted.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }
    }

    public static class NoOpCommandDispatcher implements CommandDispatcher<ProjectId> {

        private final CommandClass commandClass = CommandClass.from(BcCreateProject.class);

        @Override
        public Set<CommandClass> messageClasses() {
            return ImmutableSet.of(commandClass);
        }

        @CanIgnoreReturnValue
        @Override
        public void dispatch(CommandEnvelope envelope) {
            // Do nothing.
        }
    }

    public static class NoOpEventDispatcher implements EventDispatcher<ProjectId> {

        private final EventClass eventClass = EventClass.from(BcProjectCreated.class);

        @Override
        public Set<EventClass> externalEventClasses() {
            return ImmutableSet.of(eventClass);
        }

        @Override
        public Set<EventClass> messageClasses() {
            return ImmutableSet.of();
        }

        @CanIgnoreReturnValue
        @Override
        public void dispatch(EventEnvelope envelope) {
            // Do nothing.
        }

        @Override
        public Optional<ExternalMessageDispatcher<ProjectId>> createExternalDispatcher() {
            return Optional.empty();
        }
    }
}
