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

package io.spine.server.aggregate;

import com.google.common.testing.NullPointerTester;
import io.spine.base.CommandMessage;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.part.AnAggregateRoot;
import io.spine.server.aggregate.given.part.TaskDescriptionPart;
import io.spine.server.aggregate.given.part.TaskDescriptionRepository;
import io.spine.server.aggregate.given.part.TaskPart;
import io.spine.server.aggregate.given.part.TaskRepository;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.aggregate.AggregateMessageDispatcher;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testdata.Sample.builderForType;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AggregatePart should")
class AggregatePartTest {

    private static final String TASK_DESCRIPTION = "Description";
    private static final TestActorRequestFactory factory =
            new TestActorRequestFactory(AggregatePartTest.class);
    private BoundedContext boundedContext;
    private AnAggregateRoot root;
    private TaskPart taskPart;
    private TaskDescriptionPart taskDescriptionPart;
    private TaskRepository taskRepository;

    private static CommandEnvelope env(CommandMessage commandMessage) {
        return CommandEnvelope.of(factory.command()
                                         .create(commandMessage));
    }

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        root = new AnAggregateRoot(boundedContext, newUuid());
        taskPart = new TaskPart(root);
        prepareAggregatePart();
        taskDescriptionPart = new TaskDescriptionPart(root);
        taskRepository = new TaskRepository();
        TaskDescriptionRepository taskDescriptionRepository = new TaskDescriptionRepository();
        boundedContext.register(taskRepository);
        boundedContext.register(taskDescriptionRepository);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() throws NoSuchMethodException {
        createNullPointerTester()
                .testStaticMethods(AggregatePart.class, NullPointerTester.Visibility.PACKAGE);
        createNullPointerTester().testAllPublicInstanceMethods(taskPart);
    }

    @Test
    @DisplayName("return aggregate part state by class")
    void returnAggregatePartStateByClass() {
        taskRepository.store(taskPart);
        Task task = taskDescriptionPart.getPartState(Task.class);
        assertEquals(TASK_DESCRIPTION, task.getDescription());
    }

    private NullPointerTester createNullPointerTester() throws NoSuchMethodException {
        Constructor constructor =
                AnAggregateRoot.class.getDeclaredConstructor(BoundedContext.class, String.class);
        NullPointerTester tester = new NullPointerTester();
        tester.setDefault(Constructor.class, constructor)
              .setDefault(BoundedContext.class, boundedContext)
              .setDefault(AggregateRoot.class, root);
        return tester;
    }

    private void prepareAggregatePart() {
        AggAddTask.Builder addTask = builderForType(AggAddTask.class);
        addTask.setProjectId(ProjectId.newBuilder()
                                      .setId("agg-part-ID"))
               .build();
        AggregateMessageDispatcher.dispatchCommand(taskPart, env(addTask.build()));
    }
}
