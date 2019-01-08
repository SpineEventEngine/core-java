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

package io.spine.server.projection.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Subscribe;
import io.spine.server.entity.ThrowingValidatingBuilder;
import io.spine.server.projection.Projection;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * @author Alex Tymchenko
 * @author Dmytro Kuzmin
 */
public class ProjectionTransactionTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProjectionTransactionTestEnv() {
    }

    @SuppressWarnings({"MethodMayBeStatic", "unused"})  // Methods accessed via reflection.
    public static class TestProjection
            extends Projection<ProjectId, Project, PatchedProjectBuilder> {

        private final List<Message> receivedEvents = newLinkedList();
        private final List<ConstraintViolation> violations;

        public TestProjection(ProjectId id) {
            this(id, null);
        }

        public TestProjection(ProjectId id, @Nullable List<ConstraintViolation> violations) {
            super(id);
            this.violations = violations;
        }

        @Override
        protected List<ConstraintViolation> checkEntityState(Project newState) {
            if (violations != null) {
                return ImmutableList.copyOf(violations);
            }
            return super.checkEntityState(newState);
        }

        @Subscribe
        public void event(PrjProjectCreated event) {
            receivedEvents.add(event);
            Project newState = Project.newBuilder(getState())
                                      .setId(event.getProjectId())
                                      .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void event(PrjTaskAdded event) {
            throw new RuntimeException("that tests the projection tx behaviour");
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
