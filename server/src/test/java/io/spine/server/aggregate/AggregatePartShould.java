/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.envelope.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregatePartTestEnv.AnAggregatePart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.AnAggregateRoot;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskDescriptionPart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskDescriptionRepository;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskPart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.TaskRepository;
import io.spine.server.aggregate.given.AggregatePartTestEnv.WrongAggregatePart;
import io.spine.server.entity.InvalidEntityStateException;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.user.User;
import io.spine.testdata.Sample;
import io.spine.validate.ConstraintViolation;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static io.spine.base.Identifier.newUuid;
import static io.spine.server.aggregate.AggregateCommandDispatcher.dispatch;
import static io.spine.server.aggregate.AggregatePart.create;
import static io.spine.server.aggregate.AggregatePart.getConstructor;
import static io.spine.test.Given.aggregatePartOfClass;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Illia Shepilov
 */
@SuppressWarnings("OverlyCoupledClass")
public class AggregatePartShould {

    private static final String TASK_DESCRIPTION = "Description";
    private static final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(AggregatePartShould.class);
    private BoundedContext boundedContext;
    private AnAggregateRoot root;
    private TaskPart taskPart;
    private TaskDescriptionPart taskDescriptionPart;
    private TaskRepository taskRepository;

    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(factory.command()
                                         .create(commandMessage));
    }

    @Before
    public void setUp() {
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
    public void not_accept_nulls_as_parameter_values() throws NoSuchMethodException {
        createNullPointerTester()
                .testStaticMethods(AggregatePart.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void not_accept_nulls_as_parameter_values_for_instance_methods()
            throws NoSuchMethodException {
        createNullPointerTester().testAllPublicInstanceMethods(taskPart);
    }

    @Test
    public void create_aggregate_part_entity() throws NoSuchMethodException {
        final Constructor<AnAggregatePart> constructor =
                AnAggregatePart.class.getDeclaredConstructor(AnAggregateRoot.class);
        final AggregatePart aggregatePart = create(constructor, root);
        assertNotNull(aggregatePart);
    }

    @Test
    public void return_aggregate_part_state_by_class() {
        taskRepository.store(taskPart);
        final Task task = taskDescriptionPart.getPartState(Task.class);
        assertEquals(TASK_DESCRIPTION, task.getDescription());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_part_does_not_have_appropriate_constructor() {
        getConstructor(WrongAggregatePart.class);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exc_during_aggregate_part_creation_when_it_does_not_have_appropriate_ctor()
            throws NoSuchMethodException {
        final Constructor<WrongAggregatePart> constructor =
                WrongAggregatePart.class.getDeclaredConstructor();
        create(constructor, root);
    }

    @Test
    public void obtain_aggregate_part_constructor() {
        final Constructor<AnAggregatePart> constructor =
                getConstructor(AnAggregatePart.class);
        assertNotNull(constructor);
    }

    @Test
    public void have_TypeInfo_utility_class() {
        assertHasPrivateParameterlessCtor(AggregatePart.TypeInfo.class);
    }

    @Test
    public void throw_InvalidEntityStateException_if_state_is_invalid() {
        final User user = User.newBuilder()
                              .setFirstName("|")
                              .setLastName("|")
                              .build();
        try {
            aggregatePartOfClass(AnAggregatePart.class).withRoot(root)
                                                       .withId(getClass().getName())
                                                       .withVersion(1)
                                                       .withState(user)
                                                       .build();
            fail();
        } catch (InvalidEntityStateException e) {
            final List<ConstraintViolation> violations = e.getError()
                                                          .getValidationError()
                                                          .getConstraintViolationList();
            assertSize(user.getAllFields().size(), violations);
        }
    }

    @Test
    public void update_valid_entity_state() {
        final User user = User.newBuilder()
                              .setFirstName("Firstname")
                              .setLastName("Lastname")
                              .build();
        aggregatePartOfClass(AnAggregatePart.class).withRoot(root)
                                                   .withId(getClass().getName())
                                                   .withVersion(1)
                                                   .withState(user)
                                                   .build();
    }

    private NullPointerTester createNullPointerTester() throws NoSuchMethodException {
        final Constructor constructor =
                AnAggregateRoot.class
                        .getDeclaredConstructor(BoundedContext.class, String.class);
        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(Constructor.class, constructor)
              .setDefault(BoundedContext.class, boundedContext)
              .setDefault(AggregateRoot.class, root);
        return tester;
    }

    private void prepareAggregatePart() {
        final AddTask addTask =
                ((AddTask.Builder) Sample.builderForType(AddTask.class))
                        .setProjectId(ProjectId.newBuilder()
                                               .setId("agg-part-ID"))
                        .build();
        dispatch(taskPart, env(addTask));
    }
}
