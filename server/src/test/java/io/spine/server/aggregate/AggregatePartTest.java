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
import io.spine.base.CommandMessage;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.part.AnAggregateRoot;
import io.spine.server.aggregate.given.part.TaskCommentsPart;
import io.spine.server.aggregate.given.part.TaskCommentsRepository;
import io.spine.server.aggregate.given.part.TaskPart;
import io.spine.server.aggregate.given.part.TaskRepository;
import io.spine.server.aggregate.given.part.TaskRoot;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.task.AggTask;
import io.spine.test.aggregate.task.AggTaskComments;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collection;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.aggregate.given.aggregate.AggregatePartTestEnv.ASSIGNEE;
import static io.spine.server.aggregate.given.aggregate.AggregatePartTestEnv.ID;
import static io.spine.server.aggregate.given.aggregate.AggregatePartTestEnv.commentTask;
import static io.spine.server.aggregate.given.aggregate.AggregatePartTestEnv.createTask;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AggregatePart should")
class AggregatePartTest {

    private static final TestActorRequestFactory factory =
            new TestActorRequestFactory(AggregatePartTest.class);

    private BoundedContext boundedContext;
    private TaskRoot root;
    private TaskPart taskPart;
    private TaskCommentsPart taskCommentsPart;
    private TaskRepository taskRepository;
    private TaskCommentsRepository taskCommentsRepository;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        root = new TaskRoot(boundedContext, ID);
        taskPart = new TaskPart(root);
        taskCommentsPart = new TaskCommentsPart(root);
        taskRepository = new TaskRepository();
        taskCommentsRepository = new TaskCommentsRepository();
        boundedContext.register(taskRepository);
        boundedContext.register(taskCommentsRepository);
        prepareAggregatePart();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() throws NoSuchMethodException {
        createNullPointerTester()
                .testStaticMethods(AggregatePart.class, PACKAGE);
        createNullPointerTester().testAllPublicInstanceMethods(taskPart);
    }

    @Test
    @DisplayName("not override other parts with common ID when querying")
    @SuppressWarnings("CheckReturnValue") // Message dispatching called for the side effect.
    void notOverrideOtherParts() {
        assertEntityCount(AggTaskComments.class, 0);
        assertEntityCount(AggTask.class, 1);

        taskCommentsRepository.dispatch(command(commentTask()));

        assertEntityCount(AggTaskComments.class, 1);
        assertEntityCount(AggTask.class, 1);
    }

    @Test
    @DisplayName("return aggregate part state by class")
    void returnAggregatePartStateByClass() {
        taskRepository.store(taskPart);
        AggTask task = taskCommentsPart.getPartState(AggTask.class);
        assertEquals(ASSIGNEE, task.getAssignee());
    }

    private void assertEntityCount(Class<? extends Message> stateType, int expectedCount) {
        Collection<? extends Message> messages = queryEntities(stateType);
        assertThat(messages).hasSize(expectedCount);
    }

    private Collection<? extends Message> queryEntities(Class<? extends Message> entityClass) {
        Query query = factory.query()
                             .all(entityClass);
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        boundedContext.stand()
                      .execute(query, observer);
        return observer.firstResponse()
                       .getMessagesList()
                       .stream()
                       .map(state -> unpack(state.getState()))
                       .collect(toList());
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

    @SuppressWarnings("CheckReturnValue")
    private void prepareAggregatePart() {
        taskRepository.dispatch(command(createTask()));
    }

    private static CommandEnvelope command(CommandMessage commandMessage) {
        return CommandEnvelope.of(factory.command()
                                         .create(commandMessage));
    }
}
