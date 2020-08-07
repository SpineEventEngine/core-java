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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Subject;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.base.Tests;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.ServerEnvironment;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.delivery.Delivery;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.type.CommandClass;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.BlackBoxId;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.blackbox.command.BbAssignSelf;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.command.BbFinalizeProject;
import io.spine.testing.server.blackbox.command.BbRegisterCommandDispatcher;
import io.spine.testing.server.blackbox.event.BbAssigneeAdded;
import io.spine.testing.server.blackbox.event.BbAssigneeRemoved;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbProjectDone;
import io.spine.testing.server.blackbox.event.BbReportCreated;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.event.BbTaskAddedToReport;
import io.spine.testing.server.blackbox.event.BbUserDeleted;
import io.spine.testing.server.blackbox.given.BbCommandDispatcher;
import io.spine.testing.server.blackbox.given.BbEventDispatcher;
import io.spine.testing.server.blackbox.given.BbInitProcess;
import io.spine.testing.server.blackbox.given.BbProjectRepository;
import io.spine.testing.server.blackbox.given.BbProjectViewProjection;
import io.spine.testing.server.blackbox.given.BbReportRepository;
import io.spine.testing.server.blackbox.given.RepositoryThrowingExceptionOnClose;
import io.spine.testing.server.blackbox.rejection.Rejections;
import io.spine.testing.server.entity.EntitySubject;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZoneOffset;
import io.spine.type.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.testing.core.given.GivenUserId.newUuid;
import static io.spine.testing.server.blackbox.given.Given.addProjectAssignee;
import static io.spine.testing.server.blackbox.given.Given.addTask;
import static io.spine.testing.server.blackbox.given.Given.assignSelf;
import static io.spine.testing.server.blackbox.given.Given.createProject;
import static io.spine.testing.server.blackbox.given.Given.createReport;
import static io.spine.testing.server.blackbox.given.Given.createdProjectState;
import static io.spine.testing.server.blackbox.given.Given.finalizeProject;
import static io.spine.testing.server.blackbox.given.Given.initProject;
import static io.spine.testing.server.blackbox.given.Given.newProjectId;
import static io.spine.testing.server.blackbox.given.Given.projectDone;
import static io.spine.testing.server.blackbox.given.Given.startProject;
import static io.spine.testing.server.blackbox.given.Given.taskAdded;
import static io.spine.testing.server.blackbox.given.Given.userDeleted;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An abstract base for integration testing of Bounded Contexts with {@link BlackBoxContext}.
 *
 * @param <T>
 *         the type of the {@code BlackBoxBoundedContext}
 */
abstract class BlackBoxContextTest<T extends BlackBoxContext> {

    private T context;

    @BeforeEach
    @OverridingMethodsMustInvokeSuper
    void setUp() {
        BoundedContextBuilder builder = newBuilder();
        builder.add(new BbProjectRepository())
               .add(new BbReportRepository())
               .add(BbProjectViewProjection.class)
               .add(BbInitProcess.class);
        @SuppressWarnings("unchecked") // see Javadoc for newBuilder().
        T ctx = (T) BlackBoxContext.from(builder);
        context = ctx;
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    /**
     * Creates a new instance of the {@code BoundedContextBuilder}.
     *
     * <p>Implementations must ensure that multi-tenancy status of the created builder
     * matches the type of the test.
     */
    abstract BoundedContextBuilder newBuilder();

    final T context() {
        return context;
    }

    @Test
    @DisplayName("ignore sent events in emitted")
    void ignoreSentEvents() {
        BbProjectId id = newProjectId();
        context.receivesCommand(createProject(id))
                .receivesEvent(taskAdded(id));
        context.assertEvent(
                BbProjectCreated
                        .newBuilder()
                        .setProjectId(id)
                        .build());
    }

    @Nested
    @DisplayName("verify state of")
    class VerifyStateOf {

        @Test
        @DisplayName("an aggregate")
        void aggregate() {
            BbCreateProject createProject = createProject();
            BbProject expectedProject = createdProjectState(createProject);
            context.receivesCommand(createProject)
                   .assertEntityWithState(createProject.getProjectId(), expectedProject.getClass())
                   .hasStateThat()
                   .isEqualTo(expectedProject);
        }

        @Test
        @DisplayName("a projection")
        void projection() {
            BbCreateProject createProject = createProject();
            BbProjectView expectedProject = createProjectView(createProject);
            context.receivesCommand(createProject)
                   .assertState(createProject.getProjectId(), expectedProject);
        }

        private BbProjectView createProjectView(BbCreateProject createProject) {
            return BbProjectView
                    .newBuilder()
                    .setId(createProject.getProjectId())
                    .build();
        }
    }

    @Test
    @DisplayName("receive and handle a single command")
    void receivesACommand() {
        context.receivesCommand(createProject())
               .assertEvents()
               .withType(BbProjectCreated.class)
               .hasSize(1);
    }

    @Test
    @DisplayName("verifiers emitting one event")
    void eventOnCommand() {
        context.receivesCommand(createProject())
               .assertEvents()
               .withType(BbProjectCreated.class)
               .hasSize(1);
    }

    @Test
    @DisplayName("receive and handle multiple commands")
    void receivesCommands() {
        BbProjectId projectId = newProjectId();
        EventSubject assertEvents = context
                .receivesCommand(createProject(projectId))
                .receivesCommands(addTask(projectId), addTask(projectId), addTask(projectId))
                .assertEvents();
        assertEvents.hasSize(4);
        assertEvents.withType(BbProjectCreated.class).hasSize(1);
        assertEvents.withType(BbTaskAdded.class).hasSize(3);
    }

    @Test
    @DisplayName("reject a command")
    void rejectsCommand() {
        BbProjectId projectId = newProjectId();
        // Create and start the project.
        context.receivesCommands(createProject(projectId), startProject(projectId));

        // Attempt to start the project again.
        context.receivesCommand(startProject(projectId))
               .assertEvents()
               .withType(Rejections.BbProjectAlreadyStarted.class)
               .hasSize(1);
    }

    @Nested
    @DisplayName("throw `AssertionError` on receiving an unsupported command")
    class FailOnUnsupportedCommand {

        /**
         * Cleans the inbox so the erroneous commands sent in tests are not persisted and do not
         * fail the other tests.
         */
        @AfterEach
        void cleanInbox() {
            ServerEnvironment.instance()
                             .use(Delivery.local(), Tests.class);
        }

        @Test
        @MuteLogging
        @DisplayName("directly from the caller")
        void fromCaller() {
            BbFinalizeProject command = finalizeProject(newProjectId());

            assertThrows(AssertionError.class, () -> context.receivesCommand(command));
        }

        @Test
        @MuteLogging
        @DisplayName("generated as a response to some other signal")
        void generatedWithinModel() {
            BbProjectDone event = projectDone(newProjectId());

            assertThrows(AssertionError.class, () -> context.receivesEvent(event));
        }
    }

    @Test
    @DisplayName("receive and react on single event")
    void receivesEvent() {
        BbProjectId projectId = newProjectId();
        EventSubject assertEvents = context
                .receivesCommand(createReport(projectId))
                .receivesEvent(taskAdded(projectId))
                .assertEvents();
        assertEvents.hasSize(2);
        assertEvents.withType(BbReportCreated.class).hasSize(1);
        assertEvents.withType(BbTaskAddedToReport.class).hasSize(1);
    }

    @Test
    @DisplayName("receive and react on multiple events")
    void receivesEvents() {
        BbProjectId projectId = newProjectId();
        EventSubject assertEvents = context
                .receivesCommand(createReport(projectId))
                .receivesEvents(taskAdded(projectId), taskAdded(projectId), taskAdded(projectId))
                .assertEvents();
        assertEvents.hasSize(4);
        assertEvents.withType(BbReportCreated.class).hasSize(1);
        assertEvents.withType(BbTaskAddedToReport.class).hasSize(3);
    }

    @Test
    @DisplayName("post an event with the default producer")
    void defaultProducer() {
        BbProjectId projectId = newProjectId();
        context.receivesEvent(taskAdded(projectId));
        ImmutableList<Event> events = context.allEvents();
        assertThat(events).hasSize(1);
        Message producer = unpack(getOnlyElement(events).getContext()
                                                        .getProducerId());
        Subject assertProducer = assertThat(producer);
        assertProducer.isInstanceOf(BlackBoxId.class);
        BlackBoxId expectedId = BlackBoxId
                .newBuilder()
                .setContextName(context.name())
                .build();
        assertProducer.isEqualTo(expectedId);
    }

    @Test
    @DisplayName("post an event with a given producer")
    void customProducer() {
        BbProjectId projectId = newProjectId();
        context.receivesEventsProducedBy(projectId, taskAdded(projectId));
        ImmutableList<Event> events = context.allEvents();
        assertThat(events).hasSize(1);
        Message producer = unpack(getOnlyElement(events).getContext()
                                                        .getProducerId());
        Subject assertProducer = assertThat(producer);
        assertProducer.isInstanceOf(BbProjectId.class);
        assertProducer.isEqualTo(projectId);
    }

    @Nested
    @DisplayName("send")
    class SendExternalEvents {

        @Test
        @DisplayName(" an external event")
        void single() {
            BbProjectId projectId = newProjectId();
            UserId user = newUuid();

            EventSubject assertEvents = context
                    .receivesCommand(createProject(projectId))
                    .receivesCommand(addProjectAssignee(projectId, user))
                    .receivesExternalEvent(userDeleted(user, projectId))
                    .assertEvents();
            assertEvents.hasSize(4);
            assertEvents.withType(BbProjectCreated.class).isNotEmpty();
            assertEvents.withType(BbAssigneeAdded.class).isNotEmpty();
            assertEvents.withType(BbUserDeleted.class).isNotEmpty();
            assertEvents.withType(BbAssigneeRemoved.class).isNotEmpty();
        }

        @Test
        @DisplayName("multiple external events")
        void multiple() {
            BbProjectId projectId = newProjectId();
            UserId user1 = newUuid();
            UserId user2 = newUuid();
            UserId user3 = newUuid();

            EventSubject assertEvents = context
                    .receivesCommand(createProject(projectId))
                    .receivesCommands(addProjectAssignee(projectId, user1),
                                      addProjectAssignee(projectId, user2),
                                      addProjectAssignee(projectId, user3))
                    .receivesExternalEvents(userDeleted(user1, projectId),
                                            userDeleted(user2, projectId),
                                            userDeleted(user3, projectId))
                    .assertEvents();
            assertEvents.hasSize(10);
            assertEvents.withType(BbProjectCreated.class).hasSize(1);
            assertEvents.withType(BbAssigneeAdded.class).hasSize(3);
            assertEvents.withType(BbAssigneeRemoved.class).hasSize(3);
            assertEvents.withType(BbUserDeleted.class).hasSize(3);
        }
    }

    @Test
    @DisplayName("throw Illegal State Exception on Bounded Context close error")
    void throwIllegalStateExceptionOnClose() {
        RepositoryThrowingExceptionOnClose throwingRepo = new RepositoryThrowingExceptionOnClose() {
            @Override
            protected void throwException() {
                throw new RuntimeException("Expected error");
            }
        };

        BlackBoxContext ctx = BlackBoxContext.from(
                newBuilder().add(throwingRepo)
        );

        assertThrows(IllegalStateException.class, ctx::close);
    }

    @Nested
    @DisplayName("create an instance by BoundedContextBuilder")
    class CreateByBuilder {

        private final ImmutableList<Repository<?, ?>> repositories = ImmutableList.of(
                new BbProjectRepository(),
                DefaultRepository.of(BbProjectViewProjection.class)
        );

        private final CommandClass commandClass =
                CommandClass.from(BbRegisterCommandDispatcher.class);
        private CommandDispatcher commandDispatcher;
        private EventDispatcher eventDispatcher;

        private final Set<TypeName> types = toTypes(repositories);

        private BlackBoxContext blackBox;
        private EventEnricher enricher;

        @BeforeEach
        void setUp() {
            enricher = EventEnricher
                    .newBuilder()
                    .build();
            commandDispatcher = new BbCommandDispatcher(commandClass);
            eventDispatcher = new BbEventDispatcher();
        }

        @Test
        void singleTenant() {
            BoundedContextBuilder builder = BoundedContextBuilder
                    .assumingTests(false)
                    .enrichEventsUsing(enricher);
            assertBlackBox(builder, SingleTenantContext.class);
        }

        @Test
        void multiTenant() {
            BoundedContextBuilder builder = BoundedContextBuilder
                    .assumingTests(true)
                    .enrichEventsUsing(enricher);
            assertBlackBox(builder, MultiTenantContext.class);
        }

        private void assertBlackBox(BoundedContextBuilder builder,
                                    Class<? extends BlackBoxContext> clazz) {
            repositories.forEach(builder::add);
            builder.addCommandDispatcher(commandDispatcher);
            builder.addEventDispatcher(eventDispatcher);
            blackBox = BlackBoxContext.from(builder);

            assertThat(blackBox).isInstanceOf(clazz);
            assertRepositories();
            assertEntityTypes();
            assertDispatchers();
            assertEnricher();
        }

        private void assertRepositories() {
            for (Repository<?, ?> repository : repositories) {
                Class<? extends EntityState> stateClass =
                        repository.entityModelClass()
                                  .stateClass();
                assertDoesNotThrow(() -> context.repositoryOf(stateClass));
            }
        }

        private void assertEntityTypes() {
            Set<TypeName> allStateTypes = context().stateTypes();
            assertThat(allStateTypes).containsAtLeastElementsIn(types);
        }

        private void assertDispatchers() {
            assertThat(commandBus().registeredCommandClasses()).contains(commandClass);
            assertThat(eventBus().registeredEventClasses())
                    .containsAtLeastElementsIn(eventDispatcher.eventClasses());
        }

        private BoundedContext context() {
            return blackBox.context();
        }

        private EventBus eventBus() {
            return context().eventBus();
        }

        private CommandBus commandBus() {
            return context().commandBus();
        }

        private void assertEnricher() {
            assertThat(eventBus().enricher()).hasValue(enricher);
        }

        /**
         * Obtains the set of entity state types from the passed repositories.
         */
        private Set<TypeName> toTypes(Iterable<Repository<?, ?>> repos) {
            ImmutableSet.Builder<TypeName> builder = ImmutableSet.builder();
            repos.forEach(repository -> builder.add(repository.entityStateType()
                                                              .toTypeName()));
            return builder.build();
        }
    }

    @Nested
    @DisplayName("obtain `EntitySubject`")
    class ObtainEntitySubject {

        private BbProjectId id;

        @BeforeEach
        void getSubject() {
            id = newProjectId();
            context.receivesCommand(initProject(id, false));
        }

        @Test
        @DisplayName("via entity class")
        void entityClass() {
            EntitySubject subject = context.assertEntity(id, BbInitProcess.class);
            assertThat(subject)
                    .isNotNull();
            subject.isInstanceOf(BbInitProcess.class);
        }

        @Test
        @DisplayName("via entity state class")
        void entityStateClass() {
            EntitySubject subject = context.assertEntityWithState(id, BbInit.class);
            assertThat(subject)
                    .isNotNull();
            subject.hasStateThat()
                   .isInstanceOf(BbInit.class);
        }

        @Nested
        @DisplayName("with")
        class NestedSubjects {

            private EntitySubject assertProcessManager;

            @BeforeEach
            void getSubj() {
                assertProcessManager = context.assertEntity(id, BbInitProcess.class);
            }

            @Test
            @DisplayName("archived flag subject")
            void archivedFlag() {
                assertProcessManager.archivedFlag()
                                    .isFalse();
            }

            @Test
            @DisplayName("deleted flag subject")
            void deletedFlag() {
                assertProcessManager.deletedFlag()
                                    .isTrue();
            }

            @Test
            @DisplayName("state subject")
            void stateSubject() {
                BbInit expectedState = BbInit
                        .newBuilder()
                        .setId(id)
                        .setInitialized(true)
                        .build();
                assertProcessManager.hasStateThat()
                                    .isEqualTo(expectedState);
            }
        }
    }

    @Nested
    @DisplayName("Provide `Subject` for generated messages")
    class MessageSubjects {

        @BeforeEach
        void sendCommand() {
            BbProjectId id = newProjectId();
            context.receivesCommand(createProject(id));
        }

        @Test
        @DisplayName("`CommandSubject`")
        void commandSubject() {
            assertThat(context.assertCommands())
                    .isNotNull();
        }

        @Test
        @DisplayName("`EventSubject`")
        void eventSubject() {
            assertThat(context.assertEvents())
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("provide `Subject` for a specified `Query` result")
    void obtainQueryResultSubject() {
        BbProjectId id = newProjectId();
        context.receivesCommand(createProject(id));

        QueryFactory queryFactory = context.requestFactory()
                                           .query();
        Query query = queryFactory.all(BbProject.class);

        BbProject expected = BbProject
                .newBuilder()
                .setId(id)
                .build();
        context.assertQueryResult(query)
               .comparingExpectedFieldsOnly()
               .containsExactly(expected);
    }


    @Nested
    @DisplayName("allow to test subscription updates")
    class AssertingSubscriptionUpdates {

        private TopicFactory topic() {
            return context.requestFactory()
                          .topic();
        }

        @Test
        @DisplayName("for entity states")
        void forEntityStates() {
            Topic topic = topic().allOf(BbProject.class);
            SubscriptionFixture subscription = context.subscribeTo(topic);

            BbProjectId id = newProjectId();
            context.receivesCommand(createProject(id));

            BbProject expected = BbProject
                    .newBuilder()
                    .setId(id)
                    .build();
            subscription.assertEntityStates()
                        .comparingExpectedFieldsOnly()
                        .containsExactly(expected);
        }

        @Test
        @DisplayName("for event messages")
        void forEventMessages() {
            Topic topic = topic().allOf(BbProjectCreated.class);
            SubscriptionFixture subscription = context.subscribeTo(topic);

            BbProjectId id = newProjectId();
            context.receivesCommand(createProject(id));

            BbProjectCreated expected = BbProjectCreated
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            subscription.assertEventMessages()
                        .comparingExpectedFieldsOnly()
                        .containsExactly(expected);
        }
    }

    @Nested
    @DisplayName("produce requests with the given actor")
    class WithGivenActor {

        @Test
        @DisplayName("ID")
        void id() {
            UserId actor = GivenUserId.of("my-actor");
            BbCreateProject createProject = createProject();
            BbProjectId id = createProject.getProjectId();
            BbAssignSelf assignSelf = assignSelf(id);
            context.withActor(actor)
                   .receivesCommands(createProject, assignSelf)
                   .assertState(id,
                                BbProject.newBuilder()
                                         .setId(id)
                                         .addAssignee(actor)
                                         .buildPartial());
        }

        @Test
        @DisplayName("time zone")
        void timeZone() {
            UserId actor = GivenUserId.of("my-other-actor");
            BbCreateProject createProject = createProject();
            BbProjectId id = createProject.getProjectId();
            ZoneId zoneId = ZoneIds.of("UTC+1");
            context.withActor(actor)
                   .in(zoneId)
                   .receivesCommand(createProject)
                   .assertEntityWithState(id, BbProject.class)
                   .exists();
            EventSubject events = context.assertEvents()
                                         .withType(BbProjectCreated.class);
            events.hasSize(1);
            ActorContext context = events.actual()
                                         .get(0)
                                         .context()
                                         .actorContext();
            ZoneOffset expectedOffset = TestActorRequestFactory.toOffset(zoneId);
            assertThat(context)
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(ActorContext
                                       .newBuilder()
                                       .setActor(actor)
                                       .setZoneId(zoneId)
                                       .setZoneOffset(expectedOffset)
                                       .buildPartial());
        }

        @Test
        @DisplayName("ID and time zone")
        void idAndTimeZone() {
            BbCreateProject createProject = createProject();
            BbProjectId id = createProject.getProjectId();
            ZoneId zoneId = ZoneIds.of("UTC-1");
            context.in(zoneId)
                   .receivesCommand(createProject)
                   .assertEntityWithState(id, BbProject.class)
                   .exists();
            EventSubject events = context.assertEvents()
                                         .withType(BbProjectCreated.class);
            events.hasSize(1);
            ActorContext context = events.actual()
                                         .get(0)
                                         .context()
                                         .actorContext();
            ZoneOffset expectedOffset = TestActorRequestFactory.toOffset(zoneId);
            assertThat(context)
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(ActorContext
                                       .newBuilder()
                                       .setZoneId(zoneId)
                                       .setZoneOffset(expectedOffset)
                                       .buildPartial());
        }
    }
}
