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

package io.spine.server.event;

import io.spine.server.BoundedContext;
import io.spine.server.event.given.AbstractReactorTestEnv.DefaultUserAssigner;
import io.spine.server.event.given.AbstractReactorTestEnv.TaskDisruptor;
import io.spine.server.type.given.GivenEvent;
import io.spine.test.event.Task;
import io.spine.test.event.TaskAdded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.event.given.AbstractReactorTestEnv.defaultUserAssigner;
import static io.spine.server.event.given.AbstractReactorTestEnv.someProjectId;
import static io.spine.server.event.given.AbstractReactorTestEnv.someTask;
import static io.spine.server.event.given.AbstractReactorTestEnv.someUserId;
import static io.spine.server.event.given.AbstractReactorTestEnv.taskDisruptor;
import static junit.framework.TestCase.assertTrue;

@DisplayName("Abstract event reactor should")
public class AbstractEventReactorTest {

    private BoundedContext projectsBc;
    private BoundedContext customersBc;
    private DefaultUserAssigner defaultUserAssigner;
    private TaskDisruptor taskDisruptor;

    @BeforeEach
    void setUp() {
        projectsBc = BoundedContext.newBuilder().build();
        customersBc = BoundedContext
                .newBuilder()
                .build();
        defaultUserAssigner = defaultUserAssigner(projectsBc.eventBus(), someUserId());
        taskDisruptor = taskDisruptor(projectsBc.eventBus());
        projectsBc.registerEventDispatcher(defaultUserAssigner);
        projectsBc.registerEventDispatcher(taskDisruptor);
    }

    @Test
    @DisplayName("react to domestic events")
    void reactToDomesticEvents() {
        Task taskToAdd = someTask();
        TaskAdded taskAdded = TaskAdded
                .newBuilder()
                .setProjectId(someProjectId())
                .setTask(taskToAdd)
                .build();
        projectsBc.eventBus()
                  .post(GivenEvent.withMessage(taskAdded));
        assertTrue(defaultUserAssigner.assignedByThisAssigner()
                                      .contains(taskToAdd));
    }

    @Test
    @DisplayName("react to external events")
    void reactToExternalEvents(){

    }
}
