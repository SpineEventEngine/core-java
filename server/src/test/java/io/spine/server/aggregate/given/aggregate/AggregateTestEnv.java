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

package io.spine.server.aggregate.given.aggregate;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.entity.rejection.StandardRejections.CannotModifyDeletedEntity;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.aggregate.command.AggAssignTask;
import io.spine.test.aggregate.command.AggCreateTask;
import io.spine.test.aggregate.command.AggReassignTask;
import io.spine.test.aggregate.task.AggTaskId;
import io.spine.testdata.Sample;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.command.TestEventFactory;
import io.spine.type.TypeUrl;

import java.util.List;

import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.testing.core.given.GivenVersion.withNumber;

/**
 * @author Alexander Yevsyukov
 * @author Mykhailo Drachuk
 */
public class AggregateTestEnv {

    /** Prevent instantiation of this test environment */
    private AggregateTestEnv() {
        // Do nothing.
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
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    private static AggTaskId newTaskId() {
        return AggTaskId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    private static UserId newUserId() {
        return UserId.newBuilder()
                     .setValue(newUuid())
                     .build();
    }

    public static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    /**
     * Creates a new multitenant bounded context with a registered
     * {@linkplain TaskAggregateRepository task repository}.
     */
    public static BoundedContext newTaskBoundedContext() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setMultitenant(true)
                                                            .build();
        boundedContext.register(new TaskAggregateRepository());
        return boundedContext;
    }

    public static AggCreateTask createTask() {
        return AggCreateTask.newBuilder()
                            .setTaskId(newTaskId())
                            .build();
    }

    public static AggAssignTask assignTask() {
        return AggAssignTask.newBuilder()
                            .setTaskId(newTaskId())
                            .setAssignee(newUserId())
                            .build();
    }

    public static AggReassignTask reassignTask() {
        return AggReassignTask.newBuilder()
                              .setTaskId(newTaskId())
                              .setAssignee(newUserId())
                              .build();
    }

    /**
     * Obtains the {@link TypeUrl} of the message from the provided event.
     */
    public static TypeUrl typeUrlOf(Event event) {
        final Any message = event.getMessage();
        final TypeUrl result = TypeUrl.parse(message.getTypeUrl());
        return result;
    }

    public static Command command(Message commandMessage, TenantId tenantId) {
        return requestFactory(tenantId).command()
                                       .create(commandMessage);
    }

    public static Command command(Message commandMessage) {
        return requestFactory().command()
                               .create(commandMessage);
    }

    public static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(command(commandMessage));
    }

    public static Event event(Message eventMessage, int versionNumber) {
        return eventFactory().createEvent(eventMessage, withNumber(versionNumber));
    }

    public static RejectionEnvelope
    cannotModifyDeletedEntity(Class<? extends Message> commandMessageCls) {
        final CannotModifyDeletedEntity rejectionMsg = CannotModifyDeletedEntity.newBuilder()
                                                                                .build();
        final Command command = io.spine.server.commandbus.Given.ACommand.withMessage(
                Sample.messageOfType(commandMessageCls));
        final Rejection rejection = Rejections.createRejection(rejectionMsg, command);
        return RejectionEnvelope.of(rejection);
    }

    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(AggregateTestEnv.class, tenantId);
    }

    public static TestActorRequestFactory requestFactory() {
        return TestActorRequestFactory.newInstance(AggregateTestEnv.class);
    }

    public static TestEventFactory eventFactory() {
        return TestEventFactory.newInstance(requestFactory());
    }
}
