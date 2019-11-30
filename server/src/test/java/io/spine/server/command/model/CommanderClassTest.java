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

package io.spine.server.command.model;

import io.spine.model.contexts.projects.command.SigAddTaskToProject;
import io.spine.model.contexts.projects.command.SigAssignTask;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigCreateTask;
import io.spine.model.contexts.projects.command.SigPauseTask;
import io.spine.model.contexts.projects.command.SigRemoveTaskFromProject;
import io.spine.model.contexts.projects.command.SigSetProjectOwner;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.command.SigStopTask;
import io.spine.model.contexts.projects.event.SigProjectCreated;
import io.spine.model.contexts.projects.event.SigProjectStopped;
import io.spine.model.contexts.projects.event.SigTaskDeleted;
import io.spine.model.contexts.projects.event.SigTaskMoved;
import io.spine.model.contexts.projects.rejection.ProjectRejections;
import io.spine.server.command.model.given.commander.SampleCommander;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.ServerAssertions.assertCommandsExactly;
import static io.spine.server.ServerAssertions.assertExactly;
import static io.spine.server.command.model.CommanderClass.asCommanderClass;

@DisplayName("`CommanderClass` should")
class CommanderClassTest {

    private final CommanderClass<?> commanderClass = asCommanderClass(SampleCommander.class);

    @Nested
    @DisplayName("provide classes of")
    class MessageClasses {

        @Test
        @DisplayName("transformed commands")
        void commands() {
            assertCommandsExactly(commanderClass.commands(),
                                  SigCreateTask.class,
                                  SigCreateProject.class,
                                  SigRemoveTaskFromProject.class,
                                  SigAssignTask.class);
        }

        @Test
        @DisplayName("produced commands")
        void outgoingCommands() {
            assertCommandsExactly(commanderClass.outgoingCommands(),
                            SigAddTaskToProject.class,
                            SigStartTask.class,
                            SigSetProjectOwner.class,
                            SigStopTask.class,
                            SigAssignTask.class,
                            SigPauseTask.class,
                            SigRemoveTaskFromProject.class);
        }

        @Test
        @DisplayName("thrown rejections")
        void events() {
            assertExactly(commanderClass.rejections(),
                          ProjectRejections.SigCannotCreateProject.class);
        }

        @Test
        @DisplayName("events (including external) in response to which the commander produces commands")
        void domesticEvents() {
            assertExactly(commanderClass.events(),
                          SigProjectCreated.class,
                          SigProjectStopped.class,
                          // External events
                          SigTaskDeleted.class,
                          SigTaskMoved.class);
        }

        @Test
        @DisplayName("external events in response to which the commander produces commands")
        void externalEvents() {
            assertExactly(commanderClass.externalEvents(),
                          SigTaskDeleted.class,
                          SigTaskMoved.class);
        }
    }
}
