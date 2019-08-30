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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.IterableSubject;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.core.UserId;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.ServerEnvironment;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.delivery.Delivery;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.type.CommandClass;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.VerifyingCounter;
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
import io.spine.testing.server.blackbox.given.BbCommandDispatcher;
import io.spine.testing.server.blackbox.given.BbDuplicateCommandDispatcher;
import io.spine.testing.server.blackbox.given.BbEventDispatcher;
import io.spine.testing.server.blackbox.given.BbInitProcess;
import io.spine.testing.server.blackbox.given.BbProjectRepository;
import io.spine.testing.server.blackbox.given.BbProjectViewProjection;
import io.spine.testing.server.blackbox.given.BbReportRepository;
import io.spine.testing.server.blackbox.given.BbTaskViewProjection;
import io.spine.testing.server.blackbox.given.RepositoryThrowingExceptionOnClose;
import io.spine.testing.server.blackbox.rejection.Rejections;
import io.spine.testing.server.entity.EntitySubject;
import io.spine.type.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.testing.core.given.GivenUserId.newUuid;
import static io.spine.testing.server.blackbox.given.Given.addProjectAssignee;
import static io.spine.testing.server.blackbox.given.Given.addTask;
import static io.spine.testing.server.blackbox.given.Given.createProject;
import static io.spine.testing.server.blackbox.given.Given.createReport;
import static io.spine.testing.server.blackbox.given.Given.createdProjectState;
import static io.spine.testing.server.blackbox.given.Given.eventDispatcherRegistered;
import static io.spine.testing.server.blackbox.given.Given.finalizeProject;
import static io.spine.testing.server.blackbox.given.Given.initProject;
import static io.spine.testing.server.blackbox.given.Given.newProjectId;
import static io.spine.testing.server.blackbox.given.Given.projectDone;
import static io.spine.testing.server.blackbox.given.Given.registerCommandDispatcher;
import static io.spine.testing.server.blackbox.given.Given.startProject;
import static io.spine.testing.server.blackbox.given.Given.taskAdded;
import static io.spine.testing.server.blackbox.given.Given.userDeleted;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An abstract base for integration testing of Bounded Contexts with {@link BlackBoxBoundedContext}.
 *
 * @param <T>
 *         the type of the {@code BlackBoxBoundedContext}
 */
abstract class BlackBoxBoundedContextTest<T extends BlackBoxBoundedContext<T>> {

    private T context;

    @BeforeEach
    void setUp() {
        context = newInstance().with(new BbProjectRepository(),
                                     DefaultRepository.of(BbProjectViewProjection.class),
                                     DefaultRepository.of(BbInitProcess.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    /**
     * Creates a new instance of a bounded context to be used in this test suite.
     */
    abstract BlackBoxBoundedContext<T> newInstance();

    T boundedContext() {
        return context;
    }

    @Test
    @DisplayName("register command dispatchers")
    void registerCommandDispatchers() {
        CommandClass commandTypeToDispatch = CommandClass.from(BbRegisterCommandDispatcher.class);
        BbCommandDispatcher dispatcher = new BbCommandDispatcher(commandTypeToDispatch);
        context.withHandlers(dispatcher);
        context.receivesCommand(registerCommandDispatcher(dispatcher.getClass()));
        assertThat(dispatcher.commandsDispatched()).isEqualTo(1);
    }

    @Test
    @DisplayName("throw on an attempt to register duplicate command dispatchers")
    void throwOnDuplicateCommandDispatchers() {
        CommandClass commandTypeToDispatch = CommandClass.from(BbRegisterCommandDispatcher.class);
        BbCommandDispatcher dispatcher = new BbCommandDispatcher(commandTypeToDispatch);
        context.withHandlers(dispatcher);
        BbDuplicateCommandDispatcher duplicateDispatcher =
                new BbDuplicateCommandDispatcher(commandTypeToDispatch);

        assertThrows(IllegalArgumentException.class,
                     () -> context.withHandlers(duplicateDispatcher));
    }

    @Test
    @DisplayName("throw on an attempt to register a null command dispatcher")
    void throwOnNullCommandDispatcher() {
        assertThrows(NullPointerException.class,
                     () -> context.withHandlers((CommandDispatcher) null));
    }

    @Test
    @DisplayName("throw on an attempt to register several command dispatchers one of which is null")
    void throwOnOneOfNull() {
        CommandClass commandTypeToDispatch = CommandClass.from(BbRegisterCommandDispatcher.class);
        BbCommandDispatcher dispatcher = new BbCommandDispatcher(commandTypeToDispatch);
        assertThrows(NullPointerException.class, () -> context.withHandlers(dispatcher, null));
    }

    @Test
    @DisplayName("register a repository if it's passed as command dispatcher")
    void registerRepoAsCommandDispatcher() {
        BbReportRepository repository = new BbReportRepository();
        context.withHandlers(repository);
        assertThat(context.allStateTypes()).contains(TypeName.of(BbReport.class));
    }

    @Test
    @DisplayName("register event dispatcher")
    void registerEventDispatcher() {
        BbEventDispatcher dispatcher = new BbEventDispatcher();
        context.withEventDispatchers(dispatcher);
        context.receivesEvent(eventDispatcherRegistered(dispatcher.getClass()));
        assertThat(dispatcher.eventsReceived()).isEqualTo(1);
    }

    @Test
    @DisplayName("throw on an attempt to register a null event dispatcher")
    void throwOnNullEventDispatcher() {
        assertThrows(NullPointerException.class,
                     () -> context.withEventDispatchers((EventDispatcher) null));
    }

    @Test
    @DisplayName("throw on an attempt to register several event dispatchers if one of them is null")
    void throwOnOneOfEventDispatchersIsNull() {
        BbEventDispatcher validDispatcher = new BbEventDispatcher();
        assertThrows(NullPointerException.class,
                     () -> context.withEventDispatchers(validDispatcher, null));
    }

    @Test
    @DisplayName("register a repository if it's passed as event dispatcher")
    void registerRepoAsEventDispatcher() {
        ProjectionRepository<?, ?, ?> repository =
                (ProjectionRepository<?, ?, ?>)
                        DefaultRepository.of(BbTaskViewProjection.class);
        context.withEventDispatchers(repository);
        assertThat(context.allStateTypes()).contains(TypeName.of(BbTaskView.class));
    }

    @Test
    @DisplayName("ignore sent events in emitted")
    void ignoreSentEvents() {
        BbProjectId id = newProjectId();
        EventSubject assertEvents = context
                .receivesCommand(createProject(id))
                .receivesEvent(taskAdded(id))
                .assertEvents();
        assertEvents.hasSize(1);
        assertEvents.withType(BbProjectCreated.class).isNotEmpty();
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
                   .assertEntityWithState(expectedProject.getClass(), createProject.getProjectId())
                   .hasStateThat()
                   .isEqualTo(expectedProject);
        }

        @Test
        @DisplayName("a projection")
        void projection() {
            BbCreateProject createProject = createProject();
            BbProjectView expectedProject = createProjectView(createProject);
            context.receivesCommand(createProject)
                   .assertEntityWithState(expectedProject.getClass(), createProject.getProjectId())
                   .hasStateThat()
                   .isEqualTo(expectedProject);
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
                             .configureDelivery(Delivery.local());
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
                .with(new BbReportRepository())
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
                .with(new BbReportRepository())
                .receivesCommand(createReport(projectId))
                .receivesEvents(taskAdded(projectId), taskAdded(projectId), taskAdded(projectId))
                .assertEvents();
        assertEvents.hasSize(4);
        assertEvents.withType(BbReportCreated.class).hasSize(1);
        assertEvents.withType(BbTaskAddedToReport.class).hasSize(3);
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
                    .receivesExternalEvent(newName("Users"), userDeleted(user, projectId))
                    .assertEvents();
            assertEvents.hasSize(3);
            assertEvents.withType(BbProjectCreated.class).isNotEmpty();
            assertEvents.withType(BbAssigneeAdded.class).isNotEmpty();
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
                    .receivesExternalEvents(newName("Users"),
                                            userDeleted(user1, projectId),
                                            userDeleted(user2, projectId),
                                            userDeleted(user3, projectId))
                    .assertEvents();
            assertEvents.hasSize(7);
            assertEvents.withType(BbProjectCreated.class).hasSize(1);
            assertEvents.withType(BbAssigneeAdded.class).hasSize(3);
            assertEvents.withType(BbAssigneeRemoved.class).hasSize(3);
        }
    }

    @Test
    @DisplayName("throw Illegal State Exception on Bounded Context close error")
    void throwIllegalStateExceptionOnClose() {
        assertThrows(IllegalStateException.class, () ->
                newInstance()
                        .with(new RepositoryThrowingExceptionOnClose() {
                            @Override
                            protected void throwException() {
                                throw new RuntimeException("Expected error");
                            }
                        })
                        .close());
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

        private BlackBoxBoundedContext<?> blackBox;
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
            repositories.forEach(builder::add);
            builder.addCommandDispatcher(commandDispatcher);
            builder.addEventDispatcher(eventDispatcher);
            blackBox = BlackBoxBoundedContext.from(builder);

            assertThat(blackBox).isInstanceOf(SingleTenantBlackBoxContext.class);
            assertEntityTypes();
            assertDispatchers();
            assertEnricher();
        }

        private void assertEntityTypes() {
            assertThat(blackBox.allStateTypes()).containsAtLeastElementsIn(types);
        }

        private void assertDispatchers() {
            assertThat(blackBox.commandBus().registeredCommandClasses()).contains(commandClass);
            assertThat(blackBox.eventBus().registeredEventClasses())
                    .containsAtLeastElementsIn(eventDispatcher.eventClasses());
        }

        private void assertEnricher() {
            assertThat(blackBox.eventBus().enricher()).hasValue(enricher);
        }

        @Test
        void multiTenant() {
            BoundedContextBuilder builder = BoundedContextBuilder
                    .assumingTests(true)
                    .enrichEventsUsing(enricher);
            repositories.forEach(builder::add);
            builder.addCommandDispatcher(commandDispatcher);
            builder.addEventDispatcher(eventDispatcher);
            blackBox = BlackBoxBoundedContext.from(builder);

            assertThat(blackBox).isInstanceOf(MultitenantBlackBoxContext.class);
            assertEntityTypes();
            assertDispatchers();
            assertEnricher();
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
            EntitySubject subject = context.assertEntity(BbInitProcess.class, id);
            assertThat(subject)
                    .isNotNull();
            subject.isInstanceOf(BbInitProcess.class);
        }

        @Test
        @DisplayName("via entity state class")
        void entityStateClass() {
            EntitySubject subject = context.assertEntityWithState(BbInit.class, id);
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
                assertProcessManager = context.assertEntity(BbInitProcess.class, id);
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

    @Test
    @DisplayName("provide a method for `Subscription` updates verification")
    void assertSubscriptionUpdates() {
        BbProjectId id = newProjectId();
        TopicFactory topicFactory = context.requestFactory()
                                           .topic();
        Topic topic = topicFactory.allOf(BbProject.class);

        BbProject expected = BbProject
                .newBuilder()
                .setId(id)
                .build();
        VerifyingCounter updateCounter =
                context.assertSubscriptionUpdates(
                        topic,
                        assertEachReceived -> assertEachReceived.comparingExpectedFieldsOnly()
                                                                .isEqualTo(expected)
                );
        context.receivesCommand(createProject(id));
        updateCounter.verifyEquals(1);
    }

    @Nested
    @DisplayName("Provide generated")
    class Generated {

        @BeforeEach
        void postCommands() {
            BbProjectId id = newProjectId();
            context.receivesCommand(createProject(id))
                   .receivesCommand(initProject(id, true)) ;
        }

        @Test
        @DisplayName("event messages")
        void eventMessages() {
            IterableSubject assertEventMessages = assertThat(context.eventMessages());
            assertEventMessages.isNotEmpty();
            assertEventMessages.hasSize(context.events()
                                               .size());
        }

        @Test
        @DisplayName("command messages")
        void commandMessages() {
            IterableSubject assertCommandMessages = assertThat(context.commandMessages());
            assertCommandMessages.isNotEmpty();
            assertCommandMessages.hasSize(context.commands()
                                                 .size());
        }
    }
}
