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

package io.spine.server.projection;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.model.DuplicateHandlerMethodError;
import io.spine.server.model.HandlerFieldFilterClashError;
import io.spine.server.projection.given.EntitySubscriberProjection;
import io.spine.server.projection.given.ProjectionTestEnv.DuplicateFilterProjection;
import io.spine.server.projection.given.ProjectionTestEnv.FilteringProjection;
import io.spine.server.projection.given.ProjectionTestEnv.MalformedProjection;
import io.spine.server.projection.given.ProjectionTestEnv.NoDefaultOptionProjection;
import io.spine.server.projection.given.ProjectionTestEnv.TestProjection;
import io.spine.server.projection.given.SavedString;
import io.spine.server.storage.StorageField;
import io.spine.server.type.EventClass;
import io.spine.server.type.given.GivenEvent;
import io.spine.string.StringifierRegistry;
import io.spine.string.Stringifiers;
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
import io.spine.testing.server.entity.given.Given;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.projection.given.ProjectionTestEnv.FilteringProjection.SET_A;
import static io.spine.server.projection.given.ProjectionTestEnv.FilteringProjection.SET_B;
import static io.spine.server.projection.given.ProjectionTestEnv.NoDefaultOptionProjection.ACCEPTED_VALUE;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.server.storage.VersionField.version;
import static io.spine.server.type.given.GivenEvent.withMessage;
import static io.spine.test.projection.Project.Status.STARTED;
import static io.spine.testing.TestValues.random;
import static io.spine.testing.TestValues.randomString;
import static io.spine.testing.server.projection.ProjectionEventDispatcher.dispatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link io.spine.server.projection.Projection}.
 *
 * @apiNote This class is named using the old-fashioned {@code Should} suffix to avoid the
 *         name clash with {@link io.spine.testing.server.projection.ProjectionTest ProjectionTest}
 *         class, which is a part of Testutil Server library.
 */
@DisplayName("Projection should")
class ProjectionShould {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(ProjectionShould.class);

    private TestProjection projection;

    @BeforeAll
    static void prepare() {
        StringifierRegistry.instance()
                           .register(Stringifiers.forInteger(), Integer.TYPE);
    }

    @BeforeEach
    void setUp() {
        String id = newUuid();
        projection = Given.projectionOfClass(TestProjection.class)
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
        dispatch(projection, stringEvent, EventContext.getDefaultInstance());
        assertTrue(projection.state()
                             .getValue()
                             .contains(stringEvent.getValue()));
        assertTrue(projection.isChanged());

        Int32Imported integerEvent = Int32Imported
                .newBuilder()
                .setValue(42)
                .build();
        dispatch(projection, integerEvent, EventContext.getDefaultInstance());
        assertTrue(projection.state()
                             .getValue()
                             .contains(String.valueOf(integerEvent.getValue())));
        assertTrue(projection.isChanged());
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
        Project aggregateState = Project
                .newBuilder()
                .setId(id)
                .setName(projectName)
                .setStatus(STARTED)
                .addTask(task)
                .build();
        MessageId entityId = MessageId
                .newBuilder()
                .setTypeUrl(TypeUrl.of(aggregateState).value())
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
        assertThrows(IllegalStateException.class,
                     () -> dispatch(projection,
                                    GivenEvent.message(),
                                    EventContext.getDefaultInstance()));
    }

    @Test
    @DisplayName("return handled event classes")
    void exposeEventClasses() {
        Set<EventClass> classes =
                asProjectionClass(TestProjection.class).domesticEvents();

        assertEquals(TestProjection.HANDLING_EVENT_COUNT, classes.size());
        assertTrue(classes.contains(EventClass.from(StringImported.class)));
        assertTrue(classes.contains(EventClass.from(Int32Imported.class)));
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
    @DisplayName("subscribe to events with specific field values")
    void subscribeToEventsWithSpecificFields() {
        ProjectId id = newId();
        StringImported setB = StringImported
                .newBuilder()
                .setValue(SET_B)
                .build();
        StringImported setA = StringImported
                .newBuilder()
                .setValue(SET_A)
                .build();
        StringImported setText = StringImported
                .newBuilder()
                .setValue("Test project name")
                .build();
        FilteringProjection projection =
                Given.projectionOfClass(FilteringProjection.class)
                     .withId(id.getId())
                     .withVersion(42)
                     .withState(SavedString.getDefaultInstance())
                     .build();
        dispatch(projection, eventFactory.createEvent(setB));
        assertThat(projection.state().getValue()).isEqualTo("B");

        dispatch(projection, eventFactory.createEvent(setA));
        assertThat(projection.state().getValue()).isEqualTo("A");

        dispatch(projection, eventFactory.createEvent(setText));
        assertThat(projection.state().getValue()).isEqualTo(setText.getValue());
    }

    @Test
    @DisplayName("fail to subscribe to the same event filtering by different fields")
    void failToSubscribeByDifferentFields() {
        assertThrows(
                HandlerFieldFilterClashError.class,
                () -> new MalformedProjection.Repository().entityClass()
        );
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

    @Test
    @DisplayName("fail on duplicate filter values")
    void failOnDuplicateFilters() {
        assertThrows(
                DuplicateHandlerMethodError.class,
                () -> new DuplicateFilterProjection.Repository().entityClass()
        );
    }

    @Test
    @DisplayName("have `version` column")
    void haveVersionColumn() {
        assertHasColumn(TestProjection.class, version, Version.class);
    }

    @Test
    @DisplayName("have `archived` and `deleted` columns")
    void haveLifecycleColumn() {
        assertHasColumn(TestProjection.class, archived, boolean.class);
        assertHasColumn(TestProjection.class, deleted, boolean.class);
    }

    private static void assertHasColumn(Class<? extends Projection<?, ?, ?>> projectionType,
                                        StorageField columnName,
                                        Class<?> columnType) {
        EntityColumnCache cache = EntityColumnCache.initializeFor(projectionType);
        EntityColumn column = cache.findColumn(columnName.toString());
        assertThat(column).isNotNull();
        assertThat(column.type()).isEqualTo(columnType);
    }

    private static ProjectId newId() {
        return ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
    }
}
