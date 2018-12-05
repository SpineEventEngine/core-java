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

package io.spine.testing.server.blackbox;

import io.spine.core.UserId;
import io.spine.testing.server.ShardingReset;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbReportCreated;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.event.BbTaskAddedToReport;
import io.spine.testing.server.blackbox.event.BbUserAssigned;
import io.spine.testing.server.blackbox.event.BbUserUnassigned;
import io.spine.testing.server.blackbox.given.BbProjectRepository;
import io.spine.testing.server.blackbox.given.BbProjectViewRepository;
import io.spine.testing.server.blackbox.given.BbReportRepository;
import io.spine.testing.server.blackbox.given.RepositoryThrowingExceptionOnClose;
import io.spine.testing.server.blackbox.rejection.Rejections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.thrice;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.acked;
import static io.spine.testing.core.given.GivenUserId.newUuid;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static io.spine.testing.server.blackbox.given.Given.addProjectAssignee;
import static io.spine.testing.server.blackbox.given.Given.addTask;
import static io.spine.testing.server.blackbox.given.Given.createProject;
import static io.spine.testing.server.blackbox.given.Given.createReport;
import static io.spine.testing.server.blackbox.given.Given.createdProjectState;
import static io.spine.testing.server.blackbox.given.Given.newProjectId;
import static io.spine.testing.server.blackbox.given.Given.startProject;
import static io.spine.testing.server.blackbox.given.Given.taskAdded;
import static io.spine.testing.server.blackbox.given.Given.userDeleted;
import static io.spine.testing.server.blackbox.verify.state.VerifyState.exactly;
import static io.spine.testing.server.blackbox.verify.state.VerifyState.exactlyOne;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An abstract base for integration testing of Bounded Contexts with {@link BlackBoxBoundedContext}.
 *
 * @param <T>
 *         the type of the {@code BlackBoxBoundedContext}
 */
@ExtendWith(ShardingReset.class)
abstract class BlackBoxBoundedContextTest<T extends BlackBoxBoundedContext<T>> {

    private T context;

    @BeforeEach
    void setUp() {
        context = newInstance().with(new BbProjectRepository(),
                                     new BbProjectViewRepository());
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    /**
     * Creates a new instance of a bounded context to be used in this test suite.
     */
    abstract BlackBoxBoundedContext<T> newInstance();

    T boundedContext() {
        return context;
    }

    @Nested
    @DisplayName("verify state of")
    class VerifyStateOf {

        @Test
        @DisplayName("a single aggregate")
        void aggregate() {
            BbCreateProject createProject = createProject();
            BbProject expectedProject = createdProjectState(createProject);
            context.receivesCommand(createProject)
                   .assertThat(exactlyOne(expectedProject));
        }

        @Test
        @DisplayName("several aggregates")
        void aggregates() {
            BbCreateProject createProject1 = createProject();
            BbCreateProject createProject2 = createProject();
            BbProject expectedProject1 = createdProjectState(createProject1);
            BbProject expectedProject2 = createdProjectState(createProject2);
            context.receivesCommands(createProject1, createProject2)
                   .assertThat(exactly(BbProject.class, of(expectedProject1, expectedProject2)));
        }

        @Test
        @DisplayName("a single projection")
        void projection() {
            BbCreateProject createProject = createProject();
            BbProjectView expectedProject = createProjectView(createProject);
            context.receivesCommand(createProject)
                   .assertThat(exactlyOne(expectedProject));
        }

        @Test
        @DisplayName("several projections")
        void projections() {
            BbCreateProject createProject1 = createProject();
            BbCreateProject createProject2 = createProject();
            BbProjectView expectedProject1 = createProjectView(createProject1);
            BbProjectView expectedProject2 = createProjectView(createProject2);
            context.receivesCommands(createProject1, createProject2)
                   .assertThat(exactly(BbProjectView.class,
                                       of(expectedProject1, expectedProject2)));
        }

        private BbProjectView createProjectView(BbCreateProject createProject) {
            return BbProjectViewVBuilder.newBuilder()
                                        .setId(createProject.getProjectId())
                                        .build();
        }
    }

    @Test
    @DisplayName("receive and handle a single command")
    void receivesACommand() {
        context.receivesCommand(createProject())
               .assertThat(acked(once()).withoutErrorsOrRejections())
               .assertThat(emittedEvent(BbProjectCreated.class, once()));
    }

    @Test
    @DisplayName("verifiers emitting one event")
    void eventOnCommand() {
        context.receivesCommand(createProject())
               .assertEmitted(BbProjectCreated.class);
    }

    @Test
    @DisplayName("receive and handle multiple commands")
    void receivesCommands() {
        BbProjectId projectId = newProjectId();
        context.receivesCommand(createProject(projectId))
               .receivesCommands(addTask(projectId), addTask(projectId), addTask(projectId))
               .assertThat(acked(count(4)).withoutErrorsOrRejections())
               .assertThat(emittedEvent(count(4)))
               .assertThat(emittedEvent(BbProjectCreated.class, once()))
               .assertThat(emittedEvent(BbTaskAdded.class, thrice()));
    }

    @Test
    @DisplayName("reject a command")
    void rejectsCommand() {
        BbProjectId projectId = newProjectId();
        // Create and start the project.
        context.receivesCommands(createProject(projectId), startProject(projectId));

        // Attempt to start the project again.
        context.receivesCommand(startProject(projectId))
               .assertRejectedWith(Rejections.BbProjectAlreadyStarted.class);
    }

    @Test
    @DisplayName("receive and react on single event")
    void receivesEvent() {
        BbProjectId projectId = newProjectId();
        context.with(new BbReportRepository())
               .receivesCommand(createReport(projectId))
               .receivesEvent(taskAdded(projectId))
               .assertThat(acked(twice()).withoutErrorsOrRejections())
               .assertThat(emittedEvent(thrice()))
               .assertThat(emittedEvent(BbReportCreated.class, once()))
               .assertThat(emittedEvent(BbTaskAddedToReport.class, once()));
    }

    @Test
    @DisplayName("receive and react on multiple events")
    void receivesEvents() {
        BbProjectId projectId = newProjectId();
        context.with(new BbReportRepository())
               .receivesCommand(createReport(projectId))
               .receivesEvents(taskAdded(projectId), taskAdded(projectId), taskAdded(projectId))
               .assertThat(acked(count(4)).withoutErrorsOrRejections())
               .assertThat(emittedEvent(count(7)))
               .assertThat(emittedEvent(BbReportCreated.class, once()))
               .assertThat(emittedEvent(BbTaskAddedToReport.class, thrice()));
    }

    @Test
    @DisplayName("receives external event")
    void receivesExternalEvents() {
        BbProjectId projectId = newProjectId();
        UserId id = newUuid();

        context.receivesCommand(createProject(projectId))
               .receivesCommand(addProjectAssignee(projectId, id))
               .receivesExternalEvent(userDeleted(id, projectId))
               .assertThat(acked(count(3)).withoutErrorsOrRejections())
               .assertThat(VerifyEvents.emittedEvent(count(3)))
               .assertThat(VerifyEvents.emittedEvent(BbProjectCreated.class, once()))
               .assertThat(VerifyEvents.emittedEvent(BbUserAssigned.class, once()))
               .assertThat(VerifyEvents.emittedEvent(BbUserUnassigned.class, once()));
    }

    @Test
    @DisplayName("throw Illegal State Exception on Bounded Context close error")
    void throwIllegalStateExceptionOnClose() {
        assertThrows(IllegalStateException.class, () ->
                newInstance()
                        .with(new RepositoryThrowingExceptionOnClose() {
                            @Override
                            protected void throwException() {
                                throw new RuntimeException("Expected error");
                            }
                        })
                        .close());
    }
}
