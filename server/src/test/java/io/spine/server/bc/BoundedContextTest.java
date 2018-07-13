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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.Responses;
import io.spine.grpc.MemoizingObserver;
import io.spine.option.EntityOption;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.bc.given.BoundedContextTestEnv.AnotherProjectAggregateRepository;
import io.spine.server.bc.given.BoundedContextTestEnv.ProjectAggregateRepository;
import io.spine.server.bc.given.BoundedContextTestEnv.ProjectPmRepo;
import io.spine.server.bc.given.BoundedContextTestEnv.ProjectReportRepository;
import io.spine.server.bc.given.BoundedContextTestEnv.SecretProjectRepository;
import io.spine.server.bc.given.BoundedContextTestEnv.TestEventSubscriber;
import io.spine.server.bc.given.Given;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStore;
import io.spine.server.integration.IntegrationEvent;
import io.spine.server.model.ModelTests;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.test.Spy;
import io.spine.test.bc.Project;
import io.spine.test.bc.SecretProject;
import io.spine.test.bc.event.BcProjectCreated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private static final String DEFAULT_STATES_FIELD_NAME = "defaultStates";
    private static final String DEFAULT_STATE_REGISTRY_FULL_CLASS_NAME =
            "io.spine.server.model.DefaultStateRegistry$Singleton";
    private static final String DEFAULT_STATE_REGISTRY_SINGLETON_FIELD_NAME = "value";

    private final TestEventSubscriber subscriber = new TestEventSubscriber();

    private BoundedContext boundedContext;

    private boolean handlersRegistered = false;

    private static Object getObjectFromNestedEnumField(String fullClassName, String fieldName) {
        Object result = null;
        try {
            Class<?> aClass = Class.forName(fullClassName);
            Object enumConstant = aClass.getEnumConstants()[0];
            Field field = aClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            result = field.get(enumConstant);
        } catch (ClassNotFoundException e) {
            failMissingClass(fullClassName);
        } catch (NoSuchFieldException e) {
            failMissingField(fieldName);
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        }
        assertNotNull(result);
        return result;
    }

    private static void failMissingClass(String fullClassName) {
        fail("Class " + fullClassName + " not found.");
    }

    private static void failMissingField(String fieldName) {
        fail("Field " + fieldName + " should exist.");
    }

    private static void injectField(Object target, String fieldName, Object valueToInject) {
        try {
            Field defaultStates = target.getClass()
                                              .getDeclaredField(fieldName);
            defaultStates.setAccessible(true);
            defaultStates.set(target, valueToInject);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            failMissingField(fieldName);
        }
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
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
        @DisplayName("RejectionBus")
        void rejectionBus() {
            assertNotNull(boundedContext.getRejectionBus());
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
            ProjectAggregateRepository repository =
                    new ProjectAggregateRepository();
            boundedContext.register(repository);
        }

        @Test
        @DisplayName("ProcessManagerRepository")
        void processManagerRepository() {
            ModelTests.clearModel();

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
        Stand stand = Spy.ofClass(Stand.class)
                         .on(boundedContext);

        verify(stand, never()).registerTypeSupplier(any());

        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
        verify(stand).registerTypeSupplier(eq(repository));
    }

    @Test
    @DisplayName("not allow two aggregate repositories with aggregates of same state")
    void throwOnSameAggregateState() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        boundedContext.register(repository);

        AnotherProjectAggregateRepository anotherRepo =
                new AnotherProjectAggregateRepository();

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
    @DisplayName("manage event subscriber notifications")
    class ManageEventSubscriberNotifications {

        @Test
        @DisplayName("when event is valid")
        void forValidEvent() {
            registerAll();
            MemoizingObserver<Ack> observer = memoizingObserver();
            IntegrationEvent event = Given.AnIntegrationEvent.projectCreated();
            Message msg = unpack(event.getMessage());

            boundedContext.notify(event, observer);

            assertEquals(Responses.statusOk(), observer.firstResponse()
                                                       .getStatus());
            assertEquals(subscriber.getHandledEvent(), msg);
        }

        @Test
        @DisplayName("when event is invalid")
        void forInvalidEvent() {
            BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                .setMultitenant(true)
                                                                .build();

            // Unsupported message.
            Any invalidMsg = AnyPacker.pack(BcProjectCreated.getDefaultInstance());
            IntegrationEvent event =
                    Given.AnIntegrationEvent.projectCreated()
                                            .toBuilder()
                                            .setMessage(invalidMsg)
                                            .build();

            MemoizingObserver<Ack> observer = memoizingObserver();
            boundedContext.notify(event, observer);

            assertEquals(ERROR, observer.firstResponse()
                                        .getStatus()
                                        .getStatusCase());
        }
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
        ModelTests.clearModel();

        boundedContext.register(new SecretProjectRepository());

        assertFalse(boundedContext.findRepository(SecretProject.class)
                                  .isPresent());
    }

    /**
     * This test checks, whether {@code BoundedContext} properly handles the issues upon repository
     * registration.
     *
     * <p>In particular, we intentionally break an interaction between {@code Model}
     * and a {@code BoundedContext} on attempt to ensure there is an entity default state present
     * for the given repository instance.
     *
     * <p>The expected behavior of {@code BoundedContext} instance is to fail fast in case such
     * a default state is absent.
     *
     * <p>In real-life this use case can never happen given the current implementation of
     * {@code Model} and {@code BoundedContext}. However, previously such an issue was caught.
     * Therefore this test case ensures it's never happening again.
     */
    @Test
    @DisplayName("throw NPE when registering repository and default state is null")
    void throwOnRegisterWithNullDefaultState() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        Map mockMap = mock(Map.class);
        when(mockMap.get(any())).thenReturn(null);
        Object defaultStateRegistry =
                getObjectFromNestedEnumField(
                        DEFAULT_STATE_REGISTRY_FULL_CLASS_NAME,
                        DEFAULT_STATE_REGISTRY_SINGLETON_FIELD_NAME
                );
        injectField(defaultStateRegistry, DEFAULT_STATES_FIELD_NAME, mockMap);

        try {
            assertThrows(NullPointerException.class, () -> boundedContext.register(repository));
        } finally {
            // Reassign default state registry to real map to prevent failing other tests.
            Map<Class<? extends Entity>, Message> defaultState = newConcurrentMap();
            injectField(defaultStateRegistry, DEFAULT_STATES_FIELD_NAME, defaultState);
        }
    }
}
