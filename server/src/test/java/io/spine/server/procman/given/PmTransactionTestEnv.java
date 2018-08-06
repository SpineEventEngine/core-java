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

package io.spine.server.procman.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.server.entity.ThrowingValidatingBuilder;
import io.spine.server.event.React;
import io.spine.server.procman.ProcessManager;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * @author Alex Tymchenko
 */
public class PmTransactionTestEnv {

    /** Prevents instantiation of this utility class. */
    private PmTransactionTestEnv() {
    }

    @SuppressWarnings({"MethodMayBeStatic", "unused"})  // Methods accessed via reflection.
    public static class TestProcessManager
            extends ProcessManager<ProjectId, Project, PatchedProjectBuilder> {

        private final List<Message> receivedEvents = newLinkedList();
        private final @Nullable List<ConstraintViolation> violations;

        public TestProcessManager(ProjectId id) {
            this(id, null);
        }

        public TestProcessManager(ProjectId id, @Nullable List<ConstraintViolation> violations) {
            super(id);
            this.violations = violations == null
                              ? null
                              : ImmutableList.copyOf(violations);
        }

        @Override
        protected List<ConstraintViolation> checkEntityState(Project newState) {
            if (violations != null) {
                return ImmutableList.copyOf(violations);
            }
            return super.checkEntityState(newState);
        }

        @React
        public Empty event(PmProjectCreated event) {
            receivedEvents.add(event);
            Project newState = Project.newBuilder(getState())
                                      .setId(event.getProjectId())
                                      .build();
            getBuilder().mergeFrom(newState);
            return Empty.getDefaultInstance();
        }

        @React
        public Empty event(PmTaskAdded event) {
            throw new RuntimeException("that tests the tx behaviour for process manager");
        }

        public List<Message> getReceivedEvents() {
            return ImmutableList.copyOf(receivedEvents);
        }
    }

    /**
     * Custom implementation of {@code ValidatingBuilder}, which allows to simulate an error
     * during the state building.
     *
     * <p>Must be declared {@code public} to allow accessing from the
     * {@linkplain io.spine.validate.ValidatingBuilders#newInstance(Class) factory method}.
     */
    public static class PatchedProjectBuilder
            extends ThrowingValidatingBuilder<Project, Project.Builder> {

        public static PatchedProjectBuilder newBuilder() {
            return new PatchedProjectBuilder();
        }
    }
}
