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
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.CommandMessage;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.part.AnAggregateRoot;
import io.spine.server.aggregate.given.part.TaskDescriptionPart;
import io.spine.server.aggregate.given.part.TaskDescriptionRepository;
import io.spine.server.aggregate.given.part.TaskPart;
import io.spine.server.aggregate.given.part.TaskRepository;
import io.spine.server.test.shared.StringAggregate;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.testdata.Sample.builderForType;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AggregatePart should")
class AggregatePartTest {

    private static final ProjectId ID = ProjectId
            .newBuilder()
            .setId(newUuid())
            .build();

    private static final String TASK_DESCRIPTION = "Description";
    private static final TestActorRequestFactory factory =
            new TestActorRequestFactory(AggregatePartTest.class);

    private BoundedContext boundedContext;
    private AnAggregateRoot root;
    private TaskPart taskPart;
    private TaskDescriptionPart taskDescriptionPart;
    private TaskRepository taskRepository;
    private TaskDescriptionRepository taskDescriptionRepository;

    private static CommandEnvelope env(CommandMessage commandMessage) {
        return CommandEnvelope.of(factory.command()
                                         .create(commandMessage));
    }

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        root = new AnAggregateRoot(boundedContext, ID);
        taskPart = new TaskPart(root);
        taskDescriptionPart = new TaskDescriptionPart(root);
        taskRepository = new TaskRepository();
        taskDescriptionRepository = new TaskDescriptionRepository();
        boundedContext.register(taskRepository);
        boundedContext.register(taskDescriptionRepository);
        prepareAggregatePart();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() throws NoSuchMethodException {
        createNullPointerTester()
                .testStaticMethods(AggregatePart.class, NullPointerTester.Visibility.PACKAGE);
        createNullPointerTester().testAllPublicInstanceMethods(taskPart);
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "HardcodedLineSeparator"}) // For clarity.
    @Test
    @DisplayName("not override other parts with common ID when querying")
    void notOverrideOtherParts() {
        System.out.println(
                "-----------------------------------------------------------------------"
        );

        // The command to `TaskPart` was already sent in `setUp`, now there is 1 `TaskPart`
        // and 0 `TaskDescriptionPart`-s.
        queryEntities(StringAggregate.class, response -> {
            int count = response.getMessagesList()
                                .size();
            System.out.printf(
                    "Initial count of entities of `TaskDescriptionPart` type is %d\n", count
            );
            assertThat(count).isEqualTo(0);
        });
        queryEntities(Task.class, response -> {
            int count = response.getMessagesList()
                                .size();
            System.out.printf(
                    "Initial count of entities of `TaskPart` type is %d\n", count
            );
            assertThat(count).isEqualTo(1);
        });

        // Now we send the command to `TaskDescriptionPart` and it should be created in the
        // repository.
        AggCreateProject.Builder aggCreateProject = builderForType(AggCreateProject.class);
        aggCreateProject.setProjectId(ID)
                        .setName("Super Project")
                        .build();
        taskDescriptionRepository.dispatch(env(aggCreateProject.build()));

        // Querying entities reveals that there are no `TaskPart`-s now.
        queryEntities(StringAggregate.class, response -> {
            int count = response.getMessagesList()
                                .size();
            System.out.printf(
                    "Final count of entities of `TaskDescriptionPart` type is %d\n", count
            );
            assertThat(count).isEqualTo(1);
        });
        queryEntities(Task.class, response -> {
            int count = response.getMessagesList()
                                .size();
            System.out.printf(
                    "Final count of entities of `TaskPart` type is %d\n", count
            );

            // Works incorrect, will be 0.
            assertThat(count).isEqualTo(1);
        });
    }

    private void queryEntities(Class<? extends Message> entityClass,
                               Consumer<QueryResponse> onResponse) {
        TestActorRequestFactory factory = new TestActorRequestFactory(AggregatePartTest.class);

        QueryFactory queryFactory = new QueryFactory(factory);
        Query query = queryFactory.select(entityClass)
                                  .build();
        StreamObserver<QueryResponse> obs1 = new StreamObserver<QueryResponse>() {
            @Override
            public void onNext(QueryResponse value) {
                onResponse.accept(value);
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onCompleted() {
            }
        };
        boundedContext.stand()
                      .execute(query, obs1);
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
                AnAggregateRoot.class.getDeclaredConstructor(BoundedContext.class, ProjectId.class);
        NullPointerTester tester = new NullPointerTester();
        tester.setDefault(Constructor.class, constructor)
              .setDefault(BoundedContext.class, boundedContext)
              .setDefault(AggregateRoot.class, root);
        return tester;
    }

    private void prepareAggregatePart() {
        AggAddTask.Builder addTask = builderForType(AggAddTask.class);
        addTask.setProjectId(ID)
               .build();
        taskRepository.dispatch(env(addTask.build()));
    }
}
