/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.procman.given;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.aggregate.AggregateTestEnv;
import io.spine.server.command.Assign;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.procman.CommandRouted;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmNotificationSent;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.test.procman.exam.PmExamId;
import io.spine.test.procman.exam.PmProblemAnswer;
import io.spine.test.procman.exam.PmProblemId;
import io.spine.test.procman.exam.command.PmAnswerProblem;
import io.spine.test.procman.exam.command.PmStartExam;
import io.spine.testdata.Sample;
import io.spine.util.Exceptions;
import io.spine.validate.AnyVBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * @author Alexander Yevsyukov
 */
public class ProcessManagerTestEnv {

    /** Prevents instantiation on this utility class. */
    private ProcessManagerTestEnv() {
    }

    public static Command command(Message commandMessage, TenantId tenantId) {
        return requestFactory(tenantId).command()
                                       .create(commandMessage);
    }

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    public static PmExamId newExamId() {
        return PmExamId.newBuilder()
                       .setId(newUuid())
                       .build();
    }

    public static PmStartExam startExam(PmExamId id, Iterable<? extends PmProblemId> problems) {
        return PmStartExam.newBuilder()
                          .setExamId(id)
                          .addAllProblem(problems)
                          .build();
    }

    public static PmAnswerProblem answerProblem(PmExamId id, PmProblemAnswer answer) {
        return PmAnswerProblem.newBuilder()
                              .setExamId(id)
                              .setAnswer(answer)
                              .build();
    }

    public static PmProblemAnswer newProblemAnswer() {
        return newProblemAnswer(newProblemId(), true);
    }

    private static PmProblemId newProblemId() {
        return PmProblemId.newBuilder()
                          .setId(newUuid())
                          .build();
    }

    public static PmProblemAnswer newProblemAnswer(PmProblemId id, boolean correct) {
        return PmProblemAnswer.newBuilder()
                              .setProblemId(id)
                              .setCorrect(correct)
                              .build();
    }

    public static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    /**
     * Creates a new multitenant bounded context with a registered
     * {@linkplain ExamProcmanRepository exam repository}.
     */
    public static BoundedContext newExamBoundedContext() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setMultitenant(true)
                                                            .build();
        boundedContext.register(new ExamProcmanRepository());
        return boundedContext;
    }

    /**
     * A convenience method for closing the bounded context.
     *
     * <p>Instead of a checked {@link java.io.IOException IOException}, wraps any issues
     * that may occur while closing, into an {@link IllegalStateException}.
     *
     * @param boundedContext a bounded context to close
     */
    public static void closeContext(BoundedContext boundedContext) {
        checkNotNull(boundedContext);
        try {
            boundedContext.close();
        } catch (Exception e) {
            throw Exceptions.illegalStateWithCauseOf(e);
        }
    }

    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(AggregateTestEnv.class, tenantId);
    }

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    public static List<Event> readAllEvents(final BoundedContext boundedContext,
                                            TenantId tenantId) {
        final MemoizingObserver<Event> queryObserver = memoizingObserver();
        final TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                boundedContext.getEventBus()
                              .getEventStore()
                              .read(allEventsQuery(), queryObserver);
            }
        };
        operation.execute();

        final List<Event> responses = queryObserver.responses();
        return responses;
    }

    /**
     * A test Process Manager which remembers past message as its state.
     */
    public static class TestProcessManager
            extends ProcessManager<ProjectId, Any, AnyVBuilder> {

        public TestProcessManager(ProjectId id) {
            super(id);
        }

        /**
         * Injects the passed CommandBus instance via Reflection since
         * {@link #setCommandBus(CommandBus)} is package-private and this
         * test environment class is outside of the parent's class package.
         */
        public void injectCommandBus(CommandBus commandBus) {
            try {
                final Method method = ProcessManager.class.getDeclaredMethod("setCommandBus",
                                                                             CommandBus.class);
                method.setAccessible(true);
                method.invoke(this, commandBus);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        /*
         * Handled commands
         ********************/

        @Assign
        PmProjectCreated handle(PmCreateProject command, CommandContext ignored) {
            getBuilder().mergeFrom(pack(command));
            return ((PmProjectCreated.Builder) Sample.builderForType(PmProjectCreated.class))
                    .setProjectId(command.getProjectId())
                    .build();
        }

        @Assign
        PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
            getBuilder().mergeFrom(pack(command));
            return ((PmTaskAdded.Builder) Sample.builderForType(PmTaskAdded.class))
                    .setProjectId(command.getProjectId())
                    .build();
        }

        @Assign
        CommandRouted handle(PmStartProject command, CommandContext context) {
            getBuilder().mergeFrom(pack(command));

            final Message addTask = ((PmAddTask.Builder) Sample.builderForType(PmAddTask.class))
                    .setProjectId(command.getProjectId())
                    .build();
            final CommandRouted route = newRouterFor(command, context)
                    .add(addTask)
                    .routeAll();
            return route;
        }

        /*
         * Reactions on events
         ************************/

        @React
        public Empty on(PmProjectCreated event, EventContext ignored) {
            getBuilder().mergeFrom(pack(event));
            return Empty.getDefaultInstance();
        }

        @React
        public Empty on(PmTaskAdded event, EventContext ignored) {
            getBuilder().mergeFrom(pack(event));
            return Empty.getDefaultInstance();
        }

        @React
        public Message on(PmProjectStarted event, EventContext ignored) {
            getBuilder().mergeFrom(pack(event));
            return Sample.messageOfType(PmNotificationSent.class);
        }


        /*
         * Reactions on rejections
         **************************/

        @React
        Empty on(EntityAlreadyArchived rejection, PmAddTask command) {
            getBuilder().mergeFrom(pack(command));
            return Empty.getDefaultInstance();
        }

        @React
        Empty on(EntityAlreadyArchived rejection) {
            getBuilder().mergeFrom(pack(rejection));
            return Empty.getDefaultInstance();
        }
    }

    /**
     * Helper dispatcher class to verify that the Process Manager routes
     * the {@link PmAddTask} command.
     */
    public static class AddTaskDispatcher implements CommandDispatcher<Message> {

        private final List<CommandEnvelope> commands = new LinkedList<>();

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(PmAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            commands.add(envelope);
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for tests.
        public List<CommandEnvelope> getCommands() {
            return commands;
        }
    }
}
