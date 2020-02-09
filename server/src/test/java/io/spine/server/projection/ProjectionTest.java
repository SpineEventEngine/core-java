/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.projection;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.entity.given.Given;
import io.spine.server.projection.given.EntitySubscriberProjection;
import io.spine.server.projection.given.NoDefaultOptionProjection;
import io.spine.server.projection.given.SavedString;
import io.spine.server.projection.given.SavingProjection;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.system.server.DiagnosticMonitor;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.Task;
import io.spine.test.projection.TaskId;
import io.spine.test.projection.event.Int32Imported;
import io.spine.test.projection.event.StringImported;
import io.spine.testing.TestValues;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.projection.given.NoDefaultOptionProjection.ACCEPTED_VALUE;
import static io.spine.server.projection.given.dispatch.ProjectionEventDispatcher.dispatch;
import static io.spine.server.type.given.GivenEvent.withMessage;
import static io.spine.test.projection.Project.Status.STARTED;
import static io.spine.testing.TestValues.random;
import static io.spine.testing.TestValues.randomString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link io.spine.server.projection.Projection}.
 *
 * @apiNote This class is named using the old-fashioned {@code Should} suffix to avoid the
 *         name clash with {@link io.spine.testing.server.projection.ProjectionTest ProjectionTest}
 *         class, which is a part of Testutil Server library.
 */
@DisplayName("Projection should")
class ProjectionTest {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(ProjectionTest.class);

    private SavingProjection projection;

    @BeforeEach
    void setUp() {
        String id = newUuid();
        projection = Given.projectionOfClass(SavingProjection.class)
                          .withId(id)
                          .withVersion(1)
                          .withState(SavedString.newBuilder()
                                                .setId(id)
                                                .setValue("Initial state")
                                                .build())
                          .build();
    }

    @Test
    @DisplayName("handle events")
    void handleEvents() {
        StringImported stringEvent = StringImported
                .newBuilder()
                .setValue(newUuid())
                .build();
        Event strEvt = eventFactory.createEvent(stringEvent);
        dispatch(projection, stringEvent, strEvt.context());
        assertTrue(projection.state()
                             .getValue()
                             .contains(stringEvent.getValue()));
        assertTrue(projection.changed());

        Int32Imported integerEvent = Int32Imported
                .newBuilder()
                .setValue(42)
                .build();
        Event intEvt = eventFactory.createEvent(integerEvent);
        dispatch(projection, integerEvent, intEvt.context());
        assertTrue(projection.state()
                             .getValue()
                             .contains(String.valueOf(integerEvent.getValue())));
        assertTrue(projection.changed());
    }

    @Test
    @DisplayName("receive entity state updates")
    void handleStateUpdates() {
        ProjectId id = newId();
        TaskId taskId = TaskId
                .newBuilder()
                .setId(TestValues.random(1, 1_000))
                .build();
        Task task = Task
                .newBuilder()
                .setTaskId(taskId)
                .setTitle("test task " + random(42))
                .build();
        String projectName = "test project name " + randomString();
        Project.Builder stateBuilder = Project
                .newBuilder()
                .setId(id)
                .setName(projectName)
                .setStatus(STARTED)
                .addTask(task);
        Project aggregateState = stateBuilder.build();
        Project previousAggState = stateBuilder.setName("Old " + stateBuilder.getName()).build();
        MessageId entityId = MessageId
                .newBuilder()
                .setTypeUrl(aggregateState.typeUrl().value())
                .setId(pack(id))
                .setVersion(Versions.zero())
                .build();
        EventId eventId = EventId
                .newBuilder()
                .setValue(newUuid())
                .build();
        EntityStateChanged systemEvent = EntityStateChanged
                .newBuilder()
                .setEntity(entityId)
                .setOldState(pack(previousAggState))
                .setNewState(pack(aggregateState))
                .setWhen(currentTime())
                .addSignalId(MessageId.newBuilder()
                                      .setId(pack(eventId))
                                      .setTypeUrl("example.org/example.test.Event"))
                .build();
        EntitySubscriberProjection projection = new EntitySubscriberProjection(id);
        dispatch(projection, withMessage(systemEvent));
        assertThat(projection.state())
                .isEqualTo(ProjectTaskNames
                                   .newBuilder()
                                   .setProjectId(id)
                                   .setProjectName(projectName)
                                   .addTaskName(task.getTitle())
                                   .build());
    }

    @Test
    @DisplayName("throw ISE if no handler is present for event")
    void throwIfNoHandlerPresent() {
        ProjectionRepository<String, ?, ?> repository =
                (ProjectionRepository<String, ?, ?>) DefaultRepository.of(SavingProjection.class);
        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .build();
        context.register(repository);
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        context.registerEventDispatcher(monitor);
        Event event = GivenEvent.arbitrary();
        EventEnvelope envelope = EventEnvelope.of(event);
        ProjectionEndpoint<String, ?> endpoint = ProjectionEndpoint.of(repository, envelope);

        endpoint.dispatchTo(projection.id());

        List<HandlerFailedUnexpectedly> systemEvents = monitor.handlerFailureEvents();
        assertThat(systemEvents).hasSize(1);
        HandlerFailedUnexpectedly systemEvent = systemEvents.get(0);
        assertThat(systemEvent.getHandledSignal()
                              .asEventId())
                .isEqualTo(event.id());
        assertThat(systemEvent.getEntity()
                              .getId())
                .isEqualTo(Identifier.pack(projection.id()));
        assertThat(systemEvent.getError().getType())
                .isEqualTo(IllegalStateException.class.getCanonicalName());
    }

    @Test
    @DisplayName("expose `play events` operation to package")
    void exposePlayingEvents() {
        StringImported stringImported = StringImported
                .newBuilder()
                .setValue("eins zwei drei")
                .build();
        Int32Imported integerImported = Int32Imported
                .newBuilder()
                .setValue(123)
                .build();
        Version nextVersion = Versions.increment(projection.version());
        Event e1 = eventFactory.createEvent(stringImported, nextVersion);
        Event e2 = eventFactory.createEvent(integerImported, Versions.increment(nextVersion));

        boolean projectionChanged = Projection.playOn(projection, ImmutableList.of(e1, e2));

        String projectionState = projection.state().getValue();
        assertTrue(projectionChanged);
        assertTrue(projectionState.contains(stringImported.getValue()));
        assertTrue(projectionState.contains(String.valueOf(integerImported.getValue())));
    }

    @Test
    @DisplayName("not dispatch event if it does not match filters")
    void notDeliverIfNotFits() {
        NoDefaultOptionProjection projection =
                Given.projectionOfClass(NoDefaultOptionProjection.class)
                     .withId(newUuid())
                     .build();
        StringImported skipped = StringImported
                .newBuilder()
                .setValue("BBB")
                .build();
        dispatch(projection, eventFactory.createEvent(skipped));
        ProtoTruth.assertThat(projection.state())
                  // Ignore the difference in the ID field of the state which
                  // was set automatically by the tx.
                  .comparingExpectedFieldsOnly()
                  .isEqualTo(SavedString.getDefaultInstance());

        StringImported dispatched = StringImported
                .newBuilder()
                .setValue(ACCEPTED_VALUE)
                .build();
        dispatch(projection, eventFactory.createEvent(dispatched));
        assertThat(projection.state().getValue()).isEqualTo(ACCEPTED_VALUE);
    }

    private static ProjectId newId() {
        return ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
    }
}
