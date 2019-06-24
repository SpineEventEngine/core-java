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

package io.spine.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.truth.Truth8;
import io.spine.annotation.Internal;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.logging.Logging;
import io.spine.option.EntityOption;
import io.spine.server.bc.given.AnotherProjectAggregate;
import io.spine.server.bc.given.FinishedProjectProjection;
import io.spine.server.bc.given.ProjectAggregate;
import io.spine.server.bc.given.ProjectAggregateRepository;
import io.spine.server.bc.given.ProjectCreationRepository;
import io.spine.server.bc.given.ProjectProcessManager;
import io.spine.server.bc.given.ProjectProjection;
import io.spine.server.bc.given.ProjectRemovalProcman;
import io.spine.server.bc.given.ProjectReport;
import io.spine.server.bc.given.SecretProjectRepository;
import io.spine.server.bc.given.TestEventSubscriber;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.store.EventStore;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.system.server.SystemClient;
import io.spine.system.server.SystemContext;
import io.spine.test.bc.Project;
import io.spine.test.bc.ProjectId;
import io.spine.test.bc.SecretProject;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.event.given.EventStoreTestEnv.eventStore;
import static io.spine.testing.TestValues.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;

/**
 * Tests of {@link BoundedContext}.
 *
 * <p>Messages used in this test suite are defined in:
 * <ul>
 *     <li>spine/test/bc/project.proto - data types
 *     <li>spine/test/bc/command_factory_test.proto — commands
 *     <li>spine/test/bc/events.proto — events.
 * </ul>
 */
@DisplayName("BoundedContext should")
class BoundedContextTest {

    private final TestEventSubscriber subscriber = new TestEventSubscriber();

    private BoundedContext context;

    private boolean handlersRegistered = false;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        context = BoundedContextBuilder.assumingTests(true).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (handlersRegistered) {
            context.eventBus()
                   .unregister(subscriber);
        }
        context.close();
    }

    /** Registers all test repositories, handlers etc. */
    private void registerAll() {
        context.register(DefaultRepository.of(ProjectAggregate.class));
        context.eventBus()
               .register(subscriber);
        handlersRegistered = true;
    }

    @Nested
    @DisplayName("return")
    class Return {

        @Test
        @DisplayName("EventBus")
        void eventBus() {
            assertNotNull(context.eventBus());
        }

        @Test
        @DisplayName("IntegrationBus")
        void integrationBus() {
            assertNotNull(context.integrationBus());
        }

        @Test
        @DisplayName("CommandDispatcher")
        void commandDispatcher() {
            assertNotNull(context.commandBus());
        }

        @Test
        @DisplayName("multitenancy state")
        void ifSetMultitenant() {
            BoundedContext bc = BoundedContextBuilder.assumingTests(true).build();
            assertTrue(bc.isMultitenant());
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("AggregateRepository")
        void aggregateRepository() {
            registerAndAssertRepository(ProjectAggregate.class);
        }

        @Test
        @DisplayName("ProcessManagerRepository")
        void processManagerRepository() {
            registerAndAssertRepository(ProjectProcessManager.class);
        }

        @Test
        @DisplayName("ProjectionRepository")
        void projectionRepository() {
            registerAndAssertRepository(ProjectReport.class);
        }

        <I, E extends Entity<I, ?>> void registerAndAssertRepository(Class<E> cls) {
            context.register(DefaultRepository.of(cls));
            assertTrue(context.hasEntitiesOfType(cls));
        }

        @Test
        @DisplayName("DefaultRepository via passed entity class")
        void entityClass() {
            context.register(ProjectAggregate.class);
            assertTrue(context.hasEntitiesOfType(ProjectAggregate.class));
        }
    }

    @Nested
    @DisplayName("test presence of entities by")
    class EntityTypePresence {

        @Nested
        @DisplayName("entity state class for")
        class ByEntityStateClass {

            @Test
            @DisplayName("visible entities")
            void visible() {
                context.register(ProjectAggregate.class);
                assertTrue(context.hasEntitiesWithState(Project.class));
            }

            @Test
            @DisplayName("invisible entities")
            void invisible() {
                context.register(new SecretProjectRepository());
                assertTrue(context.hasEntitiesWithState(SecretProject.class));
            }
        }

        @Nested
        @DisplayName("entity class for")
        class ByEntityClass {

            @Test
            @DisplayName("visible entities")
            void visible() {
                context.register(ProjectAggregate.class);
                assertTrue(context.hasEntitiesOfType(ProjectAggregate.class));
            }

            @Test
            @DisplayName("invisible entities")
            void invisible() {
                // Process Managers are invisible by default.
                context.register(ProjectProcessManager.class);
                assertTrue(context.hasEntitiesOfType(ProjectProcessManager.class));
            }
        }
    }

    @Test
    @DisplayName("propagate registered repositories to Stand")
    void propagateRepositoriesToStand() {
        BoundedContext boundedContext = BoundedContextBuilder.assumingTests().build();
        Stand stand = boundedContext.stand();
        assertTrue(stand.getExposedTypes().isEmpty());
        Repository<ProjectId, ProjectAggregate> repo = DefaultRepository.of(ProjectAggregate.class);
        boundedContext.register(repo);
        assertThat(stand.getExposedTypes())
                .containsExactly(repo.entityStateType());
    }

    @Test
    @DisplayName("re-register Stand as event dispatcher when registering repository")
    void registerStandAsEventDispatcher() {
        EventBus eventBusMock = mock(EventBus.class);
        EventBus.Builder builderMock = mock(EventBus.Builder.class);
        when(builderMock.build()).thenReturn(eventBusMock);
        BoundedContext boundedContext = BoundedContextBuilder
                .assumingTests()
                .setEventBus(builderMock)
                .build();
        boundedContext.register(DefaultRepository.of(ProjectAggregate.class));
        verify(eventBusMock, times(1))
                .register(eq(boundedContext.stand()));
    }

    @ParameterizedTest
    @MethodSource("sameStateRepositories")
    @DisplayName("not allow two entity repositories with entities of same state")
    void throwOnSameEntityState(Repository<?, ?> firstRepo,
                                Repository<?, ?> secondRepo) {
        context.register(firstRepo);
        assertThrows(IllegalStateException.class, () -> context.register(secondRepo));
    }

    /**
     * Returns all combinations of repositories that manage entities of the same state.
     *
     * <p>To check whether a {@link io.spine.server.BoundedContext} really throws
     * an {@code IllegalStateException} upon an attempt to register a repository that manages an
     * entity of a state that a registered entity repository is already managing, all combinations
     * of entities that take state as a type parameter need to be checked.
     *
     * <p>This method returns a stream of pairs of all such combinations, which is a Cartesian
     * product of:
     * <ul>
     *     <li>{@linkplain io.spine.server.procman.ProcessManagerRepository process manager};
     *     <li>{@linkplain io.spine.server.aggregate.AggregateRepository aggregate};
     *     <li>{@linkplain io.spine.server.projection.ProjectionRepository projection}.
     * </ul>
     * All of the returned repositories manage entities of the same state type.
     */
    private static Stream<Arguments> sameStateRepositories() {
        Set<Repository<?, ?>> repositories =
                ImmutableSet.of(DefaultRepository.of(ProjectAggregate.class),
                                DefaultRepository.of(ProjectProjection.class),
                                new ProjectCreationRepository());

        Set<Repository<?, ?>> sameStateRepositories =
                ImmutableSet.of(DefaultRepository.of(AnotherProjectAggregate.class),
                                DefaultRepository.of(FinishedProjectProjection.class),
                                DefaultRepository.of(ProjectRemovalProcman.class));

        Set<List<Repository<?, ?>>> cartesianProduct =
                Sets.cartesianProduct(repositories, sameStateRepositories);
        Stream<Arguments> result =
                cartesianProduct.stream()
                                .map(repos -> Arguments.of(repos.get(0), repos.get(1)));
        return result;
    }

    @Test
    @DisplayName("assign storage during registration if repository does not have one")
    void setStorageOnRegister() {
        Repository<ProjectId, ProjectAggregate> repository =
                DefaultRepository.of(ProjectAggregate.class);
        context.register(repository);
        assertTrue(repository.isStorageAssigned());
    }

    @Test
    @DisplayName("not override storage during registration if repository has one")
    void notOverrideStorage() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        Repository spy = spy(repository);
        context.register(repository);
        verify(spy, never()).initStorage(any(StorageFactory.class));
    }

    @Test
    @DisplayName("allow custom EventBus")
    void setEventBusStorageFactory() {
        BoundedContext bc = BoundedContextBuilder
                .assumingTests()
                .setEventBus(EventBus.newBuilder())
                .build();
        assertNotNull(bc.eventBus());
    }

    @Test
    @DisplayName("not overwrite EventStore if already set in EventBus.Builder")
    void useEventStoreIfSet() {
        EventStore eventStore = eventStore();
        BoundedContext bc = BoundedContextBuilder
                .assumingTests()
                .setEventBus(EventBus.newBuilder().setEventStore(eventStore))
                .build();
        assertEquals(eventStore, bc.eventBus()
                                   .eventStore());
    }

    @Nested
    @DisplayName("match multitenancy state of")
    class MatchMultitenancyState {

        @Test
        @DisplayName("CommandBus")
        void ofCommandBus() {
            CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                      .setMultitenant(false);
            assertThrows(IllegalStateException.class,
                         () -> BoundedContextBuilder
                                 .assumingTests(true)
                                 .setCommandBus(commandBus)
                                 .build());
        }

        @Test
        @DisplayName("Stand")
        void ofStand() {
            Stand.Builder stand = Stand.newBuilder()
                                       .setMultitenant(false);
            assertThrows(IllegalStateException.class,
                         () -> BoundedContextBuilder
                                 .assumingTests(true)
                                 .setStand(stand)
                                 .build());
        }
    }

    @Nested
    @DisplayName("assign own multitenancy state to")
    class AssignMultitenancyState {

        private BoundedContext context;

        @Test
        @DisplayName("CommandBus")
        void toCommandBus() {
            context = multiTenant();
            assertMultitenancyEqual(context::isMultitenant, context.commandBus()::isMultitenant);

            context = singleTenant();
            assertMultitenancyEqual(context::isMultitenant, context.commandBus()::isMultitenant);
        }

        @Test
        @DisplayName("Stand")
        void toStand() {
            context = multiTenant();

            assertMultitenancyEqual(context::isMultitenant, context.stand()::isMultitenant);

            context = singleTenant();

            assertMultitenancyEqual(context::isMultitenant, context.stand()::isMultitenant);
        }

        private void assertMultitenancyEqual(BooleanSupplier s1, BooleanSupplier s2) {
            assertThat(s1.getAsBoolean())
                    .isEqualTo(s2.getAsBoolean());
        }

        private BoundedContext multiTenant() {
            return BoundedContextBuilder
                    .assumingTests(true)
                    .build();
        }

        private BoundedContext singleTenant() {
            return BoundedContextBuilder
                    .assumingTests(false)
                    .build();
        }
    }

    /**
     * Simply checks that the result isn't empty to cover the integration with
     * {@link io.spine.server.entity.VisibilityGuard VisibilityGuard}.
     *
     * <p>See {@linkplain io.spine.server.entity.VisibilityGuardTest tests of VisibilityGuard}
     * for how visibility filtering works.
     */
    @Test
    @DisplayName("obtain entity types by visibility")
    void getEntityTypesByVisibility() {
        assertThat(context.stateTypes(EntityOption.Visibility.FULL))
                .isEmpty();

        registerAll();

        assertThat(context.stateTypes(EntityOption.Visibility.FULL))
                .isNotEmpty();
    }

    @Test
    @DisplayName("throw ISE when no repository is found for specified entity state class")
    void throwOnNoRepoFound() {
        // Attempt to get a repository without registering.
        assertThrows(IllegalStateException.class,
                     () -> context.findRepository(Project.class));
    }

    @Test
    @DisplayName("not expose invisible aggregates")
    void notExposeInvisibleAggregates() {
        ModelTests.dropAllModels();

        context.register(new SecretProjectRepository());

        Truth8.assertThat(context.findRepository(SecretProject.class))
              .isEmpty();
    }

    @Test
    @DisplayName("prohibit 3rd party descendants")
    void noExternalDescendants() {
        assertThrows(
                IllegalStateException.class,
                () ->
                new BoundedContext(BoundedContextBuilder.assumingTests()) {
                    @SuppressWarnings("ReturnOfNull") // OK for this test dummy.
                    @Internal
                    @Override
                    public SystemClient systemClient() {
                        return null;
                    }
                }
        );
    }

    @Test
    @DisplayName("close System context when domain context is closed")
    void closeSystemWhenDomainIsClosed() throws Exception {
        BoundedContextName contextName = BoundedContextNames.newName("TestDomain");
        BoundedContextName systemContextName = BoundedContextNames.system(contextName);
        BoundedContext context = BoundedContext.singleTenant(contextName.getValue()).build();
        Queue<SubstituteLoggingEvent> log = new ArrayDeque<>();
        Logging.redirect((SubstituteLogger) Logging.get(DomainContext.class), log);
        Logging.redirect((SubstituteLogger) Logging.get(SystemContext.class), log);

        context.close();

        assertThat(log).hasSize(2);

        SubstituteLoggingEvent domainLogEvent = log.poll();
        assertThat(domainLogEvent.getMessage()).contains(contextName.getValue());
        assertThat(domainLogEvent.getLevel()).isAtLeast(DEBUG);

        SubstituteLoggingEvent systemLogEvent = log.poll();
        assertThat(systemLogEvent.getMessage()).contains(systemContextName.getValue());
        assertThat(systemLogEvent.getLevel()).isAtLeast(DEBUG);
    }

    @Test
    @DisplayName("return its name in `toString()`")
    void stringForm() {
        String name = randomString();

        assertThat(BoundedContext.singleTenant(name)
                                 .build()
                                 .toString())
                .isEqualTo(name);
    }
}
