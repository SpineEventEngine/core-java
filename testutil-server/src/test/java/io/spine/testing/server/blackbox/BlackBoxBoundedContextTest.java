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

import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbReportCreated;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.event.BbTaskAddedToReport;
import io.spine.testing.server.blackbox.given.BbProjectRepository;
import io.spine.testing.server.blackbox.given.BbReportRepository;
import io.spine.testing.server.blackbox.given.RepositoryThrowingExceptionOnClose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.thrice;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.acked;
import static io.spine.testing.server.blackbox.given.Given.addTask;
import static io.spine.testing.server.blackbox.given.Given.createProject;
import static io.spine.testing.server.blackbox.given.Given.createReport;
import static io.spine.testing.server.blackbox.given.Given.newProjectId;
import static io.spine.testing.server.blackbox.given.Given.taskAdded;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Black Box Bounded Context should")
class BlackBoxBoundedContextTest {

    private BlackBoxBoundedContext project;

    @BeforeEach
    void setUp() {
        project = BlackBoxBoundedContext.newInstance()
                                        .with(new BbProjectRepository());
    }

    @AfterEach
    void tearDown() {
        project.close();
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Test
    @DisplayName("receive and handle a single commands")
    void receivesACommand() {
        project.receivesCommand(createProject())
               .assertThat(acked(once()).withoutErrorsOrRejections())
               .assertThat(VerifyEvents.emittedEvent(BbProjectCreated.class, once()));
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Test
    @DisplayName("receive and handle multiple commands")
    void receivesCommands() {
        BbProjectId projectId = newProjectId();
        project.receivesCommand(createProject(projectId))
               .receivesCommands(addTask(projectId), addTask(projectId), addTask(projectId))
               .assertThat(acked(count(4)).withoutErrorsOrRejections())
               .assertThat(VerifyEvents.emittedEvent(count(4)))
               .assertThat(VerifyEvents.emittedEvent(BbProjectCreated.class, once()))
               .assertThat(VerifyEvents.emittedEvent(BbTaskAdded.class, thrice()));
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Test
    @DisplayName("receive and react on single event")
    void receivesEvent() {
        BbProjectId projectId = newProjectId();
        project.with(new BbReportRepository())
               .receivesCommand(createReport(projectId))
               .receivesEvent(taskAdded(projectId))
               .assertThat(acked(twice()).withoutErrorsOrRejections())
               .assertThat(VerifyEvents.emittedEvent(thrice()))
               .assertThat(VerifyEvents.emittedEvent(BbReportCreated.class, once()))
               .assertThat(VerifyEvents.emittedEvent(BbTaskAddedToReport.class, once()));
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Test
    @DisplayName("receive and react on multiple events")
    void receivesEvents() {
        BbProjectId projectId = newProjectId();
        project.with(new BbReportRepository())
               .receivesCommand(createReport(projectId))
               .receivesEvents(taskAdded(projectId), taskAdded(projectId), taskAdded(projectId))
               .assertThat(acked(count(4)).withoutErrorsOrRejections())
               .assertThat(VerifyEvents.emittedEvent(count(7)))
               .assertThat(VerifyEvents.emittedEvent(BbReportCreated.class, once()))
               .assertThat(VerifyEvents.emittedEvent(BbTaskAddedToReport.class, thrice()));
    }

    @Test
    @DisplayName("throw Illegal State Exception on Bounded Context close error")
    void throwIllegalStateExceptionOnClose() {
        assertThrows(IllegalStateException.class, () ->
                BlackBoxBoundedContext
                        .newInstance()
                        .with(new RepositoryThrowingExceptionOnClose() {
                            @Override
                            protected void throwException() {
                                throw new RuntimeException("Expected error");
                            }
                        })
                        .close());
    }
}
