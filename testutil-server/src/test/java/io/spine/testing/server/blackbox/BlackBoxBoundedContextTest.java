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
import com.google.common.truth.Truth8;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.type.CommandClass;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.command.BbRegisterCommandDispatcher;
import io.spine.testing.server.blackbox.event.BbAssigneeAdded;
import io.spine.testing.server.blackbox.event.BbAssigneeRemoved;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
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
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.thrice;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.acked;
import static io.spine.testing.core.given.GivenUserId.newUuid;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static io.spine.testing.server.blackbox.given.Given.addProjectAssignee;
import static io.spine.testing.server.blackbox.given.Given.addTask;
import static io.spine.testing.server.blackbox.given.Given.createProject;
import static io.spine.testing.server.blackbox.given.Given.createReport;
import static io.spine.testing.server.blackbox.given.Given.createdProjectState;
import static io.spine.testing.server.blackbox.given.Given.eventDispatcherRegistered;
import static io.spine.testing.server.blackbox.given.Given.initProject;
import static io.spine.testing.server.blackbox.given.Given.newProjectId;
import static io.spine.testing.server.blackbox.given.Given.registerCommandDispatcher;
import static io.spine.testing.server.blackbox.given.Given.startProject;
import static io.spine.testing.server.blackbox.given.Given.taskAdded;
import static io.spine.testing.server.blackbox.given.Given.userDeleted;
import static io.spine.testing.server.blackbox.verify.state.VerifyState.exactly;
import static io.spine.testing.server.blackbox.verify.state.VerifyState.exactlyOne;
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
        BbCommandDispatcher dispatcher = new BbCommandDispatcher(context.eventBus(),
                                                                 commandTypeToDispatch);
        context.withHandlers(dispatcher);
        context.receivesCommand(registerCommandDispatcher(dispatcher.getClass()));
        assertThat(dispatcher.commandsDispatched()).isEqualTo(1);
    }

    @Test
    @DisplayName("throw on an attempt to register duplicate command dispatchers")
    void throwOnDuplicateCommandDispatchers() {
        CommandClass commandTypeToDispatch = CommandClass.from(BbRegisterCommandDispatcher.class);
        BbCommandDispatcher dispatcher = new BbCommandDispatcher(context.eventBus(),
                                                                 commandTypeToDispatch);
        context.withHandlers(dispatcher);
        BbDuplicateCommandDispatcher duplicateDispatcher =
                new BbDuplicateCommandDispatcher(context.eventBus(),
                                                 commandTypeToDispatch);

        assertThrows(IllegalArgumentException.class,
                     () -> context.withHandlers(duplicateDispatcher));
    }

    @Test
    @DisplayName("throw on an attempt to register a null command dispatcher")
    void throwOnNullCommandDispatcher() {
        assertThrows(NullPointerException.class,
                     () -> context.withHandlers((CommandDispatcher<?>) null));
    }

    @Test
    @DisplayName("throw on an attempt to register several command dispatchers one of which is null")
    void throwOnOneOfNull() {
        CommandClass commandTypeToDispatch = CommandClass.from(BbRegisterCommandDispatcher.class);
        BbCommandDispatcher dispatcher = new BbCommandDispatcher(context.eventBus(),
                                                                 commandTypeToDispatch);
        assertThrows(NullPointerException.class, () -> context.withHandlers(dispatcher, null));
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
                     () -> context.withEventDispatchers((EventDispatcher<?>) null));
    }

    @Test
    @DisplayName("throw on an attempt to register several event dispatchers if one of them is null")
    void throwOnOneOfEventDispatchersIsNull() {
        BbEventDispatcher validDispatcher = new BbEventDispatcher();
        assertThrows(NullPointerException.class,
                     () -> context.withEventDispatchers(validDispatcher, null));
    }

    @Test
    @DisplayName("ignore sent events in emitted")
    void ignoreSentEvents() {
        BbProjectId id = newProjectId();
        context.receivesCommand(createProject(id))
               .receivesEvent(taskAdded(id))
               .assertThat(emittedEvent(once()))
               .assertThat(emittedEvent(BbProjectCreated.class, once()));
    }

    @Nested
    @DisplayName("verify state of")
    class VerifyStateOf {

        @Test
        @DisplayName("a single aggregate")
        void aggregate() {
            BbCreateProject createProject = createProject();
            BbProject expectedProject = createdProjectState(createProject);
            context.receivesCommand(createProject)
                   .assertThat(exactlyOne(expectedProject));
        }

        @Test
        @DisplayName("several aggregates")
        void aggregates() {
            BbCreateProject cmd1 = createProject();
            BbCreateProject cmd2 = createProject();
            BbProject expectedProject1 = createdProjectState(cmd1);
            BbProject expectedProject2 = createdProjectState(cmd2);
            context.receivesCommands(cmd1, cmd2)
                   .assertThat(exactly(BbProject.class,
                                       ImmutableSet.of(expectedProject1, expectedProject2)));
        }

        @Test
        @DisplayName("a single projection")
        void projection() {
            BbCreateProject createProject = createProject();
            BbProjectView expectedProject = createProjectView(createProject);
            context.receivesCommand(createProject)
                   .assertThat(exactlyOne(expectedProject));
        }

        @Test
        @DisplayName("several projections")
        void projections() {
            BbCreateProject cmd1 = createProject();
            BbCreateProject cmd2 = createProject();
            BbProjectView expectedProject1 = createProjectView(cmd1);
            BbProjectView expectedProject2 = createProjectView(cmd2);
            context.receivesCommands(cmd1, cmd2)
                   .assertThat(exactly(BbProjectView.class,
                                       ImmutableSet.of(expectedProject1, expectedProject2)));
        }

        private BbProjectView createProjectView(BbCreateProject createProject) {
            return BbProjectViewVBuilder.newBuilder()
                                        .setId(createProject.getProjectId())
                                        .build();
        }
    }

    @Test
    @DisplayName("receive and handle a single command")
    void receivesACommand() {
        context.receivesCommand(createProject())
               .assertThat(acked(once()).withoutErrorsOrRejections())
               .assertThat(emittedEvent(BbProjectCreated.class, once()));
    }

    @Test
    @DisplayName("verifiers emitting one event")
    void eventOnCommand() {
        context.receivesCommand(createProject())
               .assertEmitted(BbProjectCreated.class);
    }

    @Test
    @DisplayName("receive and handle multiple commands")
    void receivesCommands() {
        BbProjectId projectId = newProjectId();
        context.receivesCommand(createProject(projectId))
               .receivesCommands(addTask(projectId), addTask(projectId), addTask(projectId))
               .assertThat(acked(count(4)).withoutErrorsOrRejections())
               .assertThat(emittedEvent(count(4)))
               .assertThat(emittedEvent(BbProjectCreated.class, once()))
               .assertThat(emittedEvent(BbTaskAdded.class, thrice()));
    }

    @Test
    @DisplayName("reject a command")
    void rejectsCommand() {
        BbProjectId projectId = newProjectId();
        // Create and start the project.
        context.receivesCommands(createProject(projectId), startProject(projectId));

        // Attempt to start the project again.
        context.receivesCommand(startProject(projectId))
               .assertRejectedWith(Rejections.BbProjectAlreadyStarted.class);
    }

    @Test
    @DisplayName("receive and react on single event")
    void receivesEvent() {
        BbProjectId projectId = newProjectId();
        context.with(new BbReportRepository())
               .receivesCommand(createReport(projectId))
               .receivesEvent(taskAdded(projectId))
               .assertThat(acked(twice()).withoutErrorsOrRejections())
               .assertThat(emittedEvent(twice()))
               .assertThat(emittedEvent(BbReportCreated.class, once()))
               .assertThat(emittedEvent(BbTaskAddedToReport.class, once()));
    }

    @Test
    @DisplayName("receive and react on multiple events")
    void receivesEvents() {
        BbProjectId projectId = newProjectId();
        context.with(new BbReportRepository())
               .receivesCommand(createReport(projectId))
               .receivesEvents(taskAdded(projectId), taskAdded(projectId), taskAdded(projectId))
               .assertThat(acked(count(4)).withoutErrorsOrRejections())
               .assertThat(emittedEvent(count(4)))
               .assertThat(emittedEvent(BbReportCreated.class, once()))
               .assertThat(emittedEvent(BbTaskAddedToReport.class, thrice()));
    }

    @Nested
    class SendExternalEvents {

        @Test
        @DisplayName("sends an external event")
        void single() {
            BbProjectId projectId = newProjectId();
            UserId user = newUuid();

            context.receivesCommand(createProject(projectId))
                   .receivesCommand(addProjectAssignee(projectId, user))
                   .receivesExternalEvent(newName("Users"), userDeleted(user, projectId))
                   .assertThat(acked(count(3)).withoutErrorsOrRejections())
                   .assertThat(emittedEvent(count(3)))
                   .assertThat(emittedEvent(BbProjectCreated.class, once()))
                   .assertThat(emittedEvent(BbAssigneeAdded.class, once()))
                   .assertThat(emittedEvent(BbAssigneeRemoved.class, once()));
        }

        @Test
        @DisplayName("sends multiple external events")
        void multiple() {
            BbProjectId projectId = newProjectId();
            UserId user1 = newUuid();
            UserId user2 = newUuid();
            UserId user3 = newUuid();

            context.receivesCommand(createProject(projectId))
                   .receivesCommands(addProjectAssignee(projectId, user1),
                                     addProjectAssignee(projectId, user2),
                                     addProjectAssignee(projectId, user3))
                   .receivesExternalEvents(newName("Users"),
                                           userDeleted(user1, projectId),
                                           userDeleted(user2, projectId),
                                           userDeleted(user3, projectId))
                   .assertThat(acked(count(7)).withoutErrorsOrRejections())
                   .assertThat(emittedEvent(count(7)))
                   .assertThat(emittedEvent(BbProjectCreated.class, once()))
                   .assertThat(emittedEvent(BbAssigneeAdded.class, thrice()))
                   .assertThat(emittedEvent(BbAssigneeRemoved.class, thrice()));
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

    /**
     * Obtains the set of entity state types from the passed repositories.
     */
    private static Set<TypeName> toTypes(Iterable<Repository<?, ?>> repos) {
        ImmutableSet.Builder<TypeName> builder = ImmutableSet.builder();
        repos.forEach(repository -> builder.add(repository.entityStateType()
                                                          .toTypeName()));
        return builder.build();
    }

    @Nested
    @DisplayName("create an instance by BoundedContextBuilder")
    class CreateByBuilder {

        private final ImmutableList<Repository<?, ?>> repositories = ImmutableList.of(
                new BbProjectRepository(),
                DefaultRepository.of(BbProjectViewProjection.class)
        );

        private final Set<TypeName> types = toTypes(repositories);

        private BlackBoxBoundedContext<?> blackBox;
        private BoundedContextBuilder builder;
        private EventEnricher enricher;

        @BeforeEach
        void setUp() {
            enricher = EventEnricher
                    .newBuilder()
                    .build();
            builder = BoundedContext
                    .newBuilder()
                    .setEventBus(EventBus.newBuilder()
                                         .setEnricher(enricher));
            repositories.forEach(builder::add);
        }

        @Test
        void singleTenant() {
            builder.setMultitenant(false);
            blackBox = BlackBoxBoundedContext.from(builder);

            assertThat(blackBox).isInstanceOf(SingleTenantBlackBoxContext.class);
            assertEntityTypes();
            assertEnricher();
        }

        private void assertEntityTypes() {
            assertThat(blackBox.getAllEntityStateTypes()).containsAllIn(types);
        }

        private void assertEnricher() {
            Truth8.assertThat(blackBox.eventBus()
                                      .enricher())
                  .hasValue(enricher);
        }

        @Test
        void multiTenant() {
            builder.setMultitenant(true);
            blackBox = BlackBoxBoundedContext.from(builder);

            assertThat(blackBox).isInstanceOf(MultitenantBlackBoxContext.class);
            assertEntityTypes();
            assertEnricher();
        }
    }

    @Nested
    @DisplayName("obtain PmSubject with")
    class ObtainEntitySubject {

        private BbProjectId id;
        private EntitySubject assertProcessManager;

        @BeforeEach
        void getSubject() {
            id = newProjectId();
            assertProcessManager = context.receivesCommand(initProject(id))
                                          .assertEntity(BbInitProcess.class, id);
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
                    .vBuilder()
                    .setId(id)
                    .setInitialized(true)
                    .build();
            assertProcessManager.hasStateThat()
                                .isEqualTo(expectedState);
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
}
