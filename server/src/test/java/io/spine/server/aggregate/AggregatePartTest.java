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

package io.spine.server.aggregate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregatePartTestEnv.AnAggregatePart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.AnAggregateRoot;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskDescriptionPart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskDescriptionRepository;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskPart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskRepository;
import io.spine.server.entity.InvalidEntityStateException;
import io.spine.server.model.ModelTests;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.user.User;
import io.spine.testdata.Sample;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static io.spine.base.Identifier.newUuid;
import static io.spine.server.entity.given.Given.aggregatePartOfClass;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Verify.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Illia Shepilov
 */
@SuppressWarnings({"OverlyCoupledClass",
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("AggregatePart should")
class AggregatePartTest {

    private static final String TASK_DESCRIPTION = "Description";
    private static final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(AggregatePartTest.class);
    private BoundedContext boundedContext;
    private AnAggregateRoot root;
    private TaskPart taskPart;
    private TaskDescriptionPart taskDescriptionPart;
    private TaskRepository taskRepository;

    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(factory.command()
                                         .create(commandMessage));
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        root = new AnAggregateRoot(boundedContext, newUuid());
        taskPart = new TaskPart(root);
        prepareAggregatePart();
        taskDescriptionPart = new TaskDescriptionPart(root);
        taskRepository = new TaskRepository();
        final TaskDescriptionRepository taskDescriptionRepository =
                new TaskDescriptionRepository();
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
        final Task task = taskDescriptionPart.getPartState(Task.class);
        assertEquals(TASK_DESCRIPTION, task.getDescription());
    }

    @SuppressWarnings("CheckReturnValue") // Method called to throw exception.
    @Test
    @DisplayName("throw InvalidEntityStateException if entity state is invalid")
    void throwOnInvalidState() {
        final User user = User.newBuilder()
                              .setFirstName("|")
                              .setLastName("|")
                              .build();
        try {
            aggregatePartOfClass(AnAggregatePart.class)
                    .withRoot(root)
                    .withId(getClass().getName())
                    .withVersion(1)
                    .withState(user)
                    .build();
            fail("Should have thrown InvalidEntityStateException.");
        } catch (InvalidEntityStateException e) {
            List<ConstraintViolation> violations =
                    e.getError()
                     .getValidationError()
                     .getConstraintViolationList();
            assertSize(user.getAllFields()
                           .size(), violations);
        }
    }

    @Test
    @DisplayName("update valid entity state")
    void updateEntityState() {
        User user = User.newBuilder()
                        .setFirstName("Firstname")
                        .setLastName("Lastname")
                        .build();
        AnAggregatePart part = aggregatePartOfClass(AnAggregatePart.class)
                .withRoot(root)
                .withId(getClass().getName())
                .withVersion(1)
                .withState(user)
                .build();

        assertEquals(user, part.getState());
    }

    private NullPointerTester createNullPointerTester() throws NoSuchMethodException {
        Constructor constructor =
                AnAggregateRoot.class
                        .getDeclaredConstructor(BoundedContext.class, String.class);
        NullPointerTester tester = new NullPointerTester();
        tester.setDefault(Constructor.class, constructor)
              .setDefault(BoundedContext.class, boundedContext)
              .setDefault(AggregateRoot.class, root);
        return tester;
    }

    private void prepareAggregatePart() {
        AggAddTask addTask =
                ((AggAddTask.Builder) Sample.builderForType(AggAddTask.class))
                        .setProjectId(ProjectId.newBuilder()
                                               .setId("agg-part-ID"))
                        .build();
        AggregateMessageDispatcher.dispatchCommand(taskPart, env(addTask));
    }
}
