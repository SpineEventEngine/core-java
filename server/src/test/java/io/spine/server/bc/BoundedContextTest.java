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

package io.spine.server.bc;

import io.spine.annotation.Internal;
import io.spine.option.EntityOption;
import io.spine.server.BoundedContext;
import io.spine.server.bc.given.AnotherProjectAggregateRepository;
import io.spine.server.bc.given.ProjectAggregateRepository;
import io.spine.server.bc.given.ProjectPmRepo;
import io.spine.server.bc.given.ProjectReportRepository;
import io.spine.server.bc.given.SecretProjectRepository;
import io.spine.server.bc.given.TestEventSubscriber;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStore;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.system.server.SystemClient;
import io.spine.test.bc.Project;
import io.spine.test.bc.SecretProject;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests of {@link BoundedContext}.
 *
 * <p>Messages used in this test suite are defined in:
 * <ul>
 *     <li>spine/test/bc/project.proto - data types
 *     <li>spine/test/bc/command_factory_test.proto — commands
 *     <li>spine/test/bc/events.proto — events.
 * </ul>
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("BoundedContext should")
class BoundedContextTest {

    private final TestEventSubscriber subscriber = new TestEventSubscriber();

    private BoundedContext boundedContext;

    private boolean handlersRegistered = false;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (handlersRegistered) {
            boundedContext.getEventBus()
                          .unregister(subscriber);
        }
        boundedContext.close();
    }

    /** Registers all test repositories, handlers etc. */
    private void registerAll() {
        ProjectAggregateRepository repo = new ProjectAggregateRepository();
        boundedContext.register(repo);
        boundedContext.getEventBus()
                      .register(subscriber);
        handlersRegistered = true;
    }

    @Nested
    @DisplayName("return")
    class Return {

        @Test
        @DisplayName("EventBus")
        void eventBus() {
            assertNotNull(boundedContext.getEventBus());
        }

        @Test
        @DisplayName("IntegrationBus")
        void integrationBus() {
            assertNotNull(boundedContext.getIntegrationBus());
        }

        @Test
        @DisplayName("CommandDispatcher")
        void commandDispatcher() {
            assertNotNull(boundedContext.getCommandBus());
        }

        @Test
        @DisplayName("multitenancy state")
        void ifSetMultitenant() {
            BoundedContext bc = BoundedContext.newBuilder()
                                              .setMultitenant(true)
                                              .build();
            assertTrue(bc.isMultitenant());
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("AggregateRepository")
        void aggregateRepository() {
            ProjectAggregateRepository repository = new ProjectAggregateRepository();
            boundedContext.register(repository);
        }

        @Test
        @DisplayName("ProcessManagerRepository")
        void processManagerRepository() {
            ModelTests.dropAllModels();

            ProjectPmRepo repository = new ProjectPmRepo();
            boundedContext.register(repository);
        }

        @Test
        @DisplayName("ProjectionRepository")
        void projectionRepository() {
            ProjectReportRepository repository = new ProjectReportRepository();
            boundedContext.register(repository);
        }
    }

    @SuppressWarnings("unchecked") // OK for the purpose of the created Matcher.
    @Test
    @DisplayName("propagate registered repositories to Stand")
    void propagateRepositoriesToStand() {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        Stand stand = boundedContext.getStand();
        assertTrue(stand.getExposedTypes().isEmpty());
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
        assertThat(stand.getExposedTypes(), contains(repository.getEntityStateType()));
    }

    @Test
    @DisplayName("not allow two aggregate repositories with aggregates of same state")
    void throwOnSameAggregateState() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        boundedContext.register(repository);

        AnotherProjectAggregateRepository anotherRepo = new AnotherProjectAggregateRepository();

        assertThrows(IllegalStateException.class, () -> boundedContext.register(anotherRepo));
    }

    @Test
    @DisplayName("assign storage during registration if repository does not have one")
    void setStorageOnRegister() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
        assertTrue(repository.isStorageAssigned());
    }

    @Test
    @DisplayName("not override storage during registration if repository has one")
    void notOverrideStorage() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        Repository spy = spy(repository);
        boundedContext.register(repository);
        verify(spy, never()).initStorage(any(StorageFactory.class));
    }

    @Test
    @DisplayName("set storage factory for EventBus")
    void setEventBusStorageFactory() {
        BoundedContext bc = BoundedContext.newBuilder()
                                          .setEventBus(EventBus.newBuilder())
                                          .build();
        assertNotNull(bc.getEventBus());
    }

    @Test
    @DisplayName("not set storage factory for EventBus if EventStore is set")
    void useEventStoreIfSet() {
        EventStore eventStore = mock(EventStore.class);
        BoundedContext bc = BoundedContext.newBuilder()
                                          .setEventBus(EventBus.newBuilder()
                                                               .setEventStore(eventStore))
                                          .build();
        assertEquals(eventStore, bc.getEventBus()
                                   .getEventStore());
    }

    @Nested
    @DisplayName("match multitenancy state of")
    class MatchMultitenancyState {

        @Test
        @DisplayName("CommandBus")
        void ofCommandBus() {
            CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                      .setMultitenant(false);
            assertThrows(IllegalStateException.class, () -> BoundedContext.newBuilder()
                                                                          .setMultitenant(true)
                                                                          .setCommandBus(commandBus)
                                                                          .build());
        }

        @Test
        @DisplayName("Stand")
        void ofStand() {
            Stand.Builder stand = Stand.newBuilder()
                                       .setMultitenant(false);
            assertThrows(IllegalStateException.class, () -> BoundedContext.newBuilder()
                                                                          .setMultitenant(true)
                                                                          .setStand(stand)
                                                                          .build());
        }
    }

    @Nested
    @DisplayName("assign own multitenancy state to")
    class AssignMultitenancyState {

        @Test
        @DisplayName("CommandBus")
        void toCommandBus() {
            BoundedContext bc = BoundedContext.newBuilder()
                                              .setMultitenant(true)
                                              .build();

            assertEquals(bc.isMultitenant(), bc.getCommandBus()
                                               .isMultitenant());

            bc = BoundedContext.newBuilder()
                               .setMultitenant(false)
                               .build();

            assertEquals(bc.isMultitenant(), bc.getCommandBus()
                                               .isMultitenant());
        }

        @Test
        @DisplayName("Stand")
        void toStand() {
            BoundedContext bc = BoundedContext.newBuilder()
                                              .setMultitenant(true)
                                              .build();

            assertEquals(bc.isMultitenant(), bc.getStand()
                                               .isMultitenant());

            bc = BoundedContext.newBuilder()
                               .setMultitenant(false)
                               .build();

            assertEquals(bc.isMultitenant(), bc.getStand()
                                               .isMultitenant());
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
        assertTrue(boundedContext.getEntityTypes(EntityOption.Visibility.FULL)
                                 .isEmpty());

        registerAll();

        assertFalse(boundedContext.getEntityTypes(EntityOption.Visibility.FULL)
                                  .isEmpty());
    }

    @Test
    @DisplayName("throw ISE when no repository is found for specified entity state class")
    void throwOnNoRepoFound() {
        // Attempt to get a repository without registering.
        assertThrows(IllegalStateException.class,
                     () -> boundedContext.findRepository(Project.class));
    }

    @Test
    @DisplayName("not expose invisible aggregates")
    void notExposeInvisibleAggregates() {
        ModelTests.dropAllModels();

        boundedContext.register(new SecretProjectRepository());

        assertFalse(boundedContext.findRepository(SecretProject.class)
                                  .isPresent());
    }

    @Test
    @DisplayName("prohibit 3rd party descendants")
    void noExternalDescendants() {
        assertThrows(
                IllegalStateException.class,

                () ->
                new BoundedContext(BoundedContext.newBuilder()) {
                    @SuppressWarnings("ReturnOfNull") // OK for this test dummy.
                    @Internal
                    @Override
                    public SystemClient getSystemClient() {
                        return null;
                    }
                }
        );
    }
}
