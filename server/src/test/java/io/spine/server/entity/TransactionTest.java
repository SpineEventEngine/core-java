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
package io.spine.server.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.Version;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.event.EventFactory;
import io.spine.test.validation.FakeOptionFactory;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.model.ModelTests;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.core.Versions.newVersion;
import static io.spine.server.type.given.GivenEvent.withMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Base class for testing the {@linkplain Transaction transactions} for different
 * {@linkplain TransactionalEntity TransactionalEntity} implementations.
 */
@SuppressWarnings({"unused" /* JUnit nested classes. */, "ClassWithTooManyMethods"})
public abstract class TransactionTest<I,
                                      E extends TransactionalEntity<I, S, B>,
                                      S extends Message,
                                      B extends ValidatingBuilder<S>> {

    private final EventFactory eventFactory = TestEventFactory.newInstance(TransactionTest.class);

    private static ValidationException validationException() {
        ValidationException ex = new ValidationException(Lists.newLinkedList());
        return ex;
    }

    private static ImmutableList<ConstraintViolation> someViolations() {
        ConstraintViolation expectedViolation = ConstraintViolation
                .newBuilder()
                .setMsgFormat("Some violation %s")
                .addParam("1")
                .build();
        return ImmutableList.of(expectedViolation);
    }

    private static boolean checkPhase(Event event, Phase phase) {
        EventId id = event.getId();
        Message phaseId = phase.getMessageId();
        boolean equalIds = id.equals(phaseId);
        boolean isSuccessful = phase.isSuccessful();
        return equalIds && isSuccessful;
    }

    private static Version someVersion() {
        return newVersion(42, currentTime());
    }

    protected abstract Transaction<I, E, S, B> createTx(E entity);

    protected abstract
    Transaction<I, E, S, B> createTxWithState(E entity, S state, Version version);

    protected abstract
    Transaction<I, E, S, B> createTxWithListener(E entity, TransactionListener<I, E, S, B> listener);

    protected abstract E createEntity();

    protected abstract E createEntity(ImmutableList<ConstraintViolation> violations);

    protected abstract S createNewState();

    protected abstract void checkEventReceived(E entity, Event event);

    protected abstract EventMessage createEventMessage();

    /**
     * Creates an event message handling of which causes an exception in the entity
     * served by the transaction under the test.
     */
    protected abstract EventMessage createEventThatFailsInHandler();

    /**
     * Creates an event message handling of which turns the builder of the entity state
     * into the state which fails the validation.
     */
    //protected abstract EventMessage createEventThatFailsInStateTransition();

    protected abstract void applyEvent(Transaction tx, Event event);

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
    }

    @AfterEach
    void tearDown() {
        FakeOptionFactory.shouldNotFail();
    }

    @Nested
    @DisplayName("initialize")
    class Initialize {

        @Test
        @DisplayName("from entity")
        void fromEntity() {
            E entity = createEntity();
            B expectedBuilder = entity.builderFromState();
            Version expectedVersion = entity.version();
            LifecycleFlags expectedLifecycleFlags = entity.lifecycleFlags();

            Transaction<I, E, S, B> tx = createTx(entity);
            assertNotNull(tx);

            assertThat(tx.entity())
                    .isEqualTo(entity);
            // Not possible to compare `Message.Builder` instances via `equals`, so trigger `build()`.
            assertThat(tx.builder().build())
                    .isEqualTo(expectedBuilder.build());
            assertThat(tx.version())
                    .isEqualTo(expectedVersion);
            assertThat(tx.lifecycleFlags())
                    .isEqualTo(expectedLifecycleFlags);
            assertTrue(tx.isActive());
            assertThat(tx.phases())
                    .isEmpty();
        }

        @Test
        @DisplayName("from entity, state, and version")
        void fromEntityStateAndVersion() {
            E entity = createEntity();
            S newState = createNewState();
            Version newVersion = someVersion();

            assertThat(newState)
                    .isNotEqualTo(entity.state());
            assertThat(newVersion)
                    .isNotEqualTo(entity.version());

            Transaction<I, E, S, B> tx = createTxWithState(entity, newState, newVersion);

            assertThat(tx.builder().build())
                    .isEqualTo(newState);
            assertThat(tx.version())
                    .isEqualTo(newVersion);
            assertThat(newState)
                    .isNotEqualTo(entity.state());
            assertThat(newVersion)
                    .isNotEqualTo(entity.version());

            tx.commit();

            // Now test that the state and the version of the entity changed to that from the tx.
            assertThat(entity.state())
                    .isEqualTo(newState);
            assertThat(entity.version())
                    .isEqualTo(newVersion);
        }
    }

    @Test
    @DisplayName("propagate changes to entity applier methods")
    void propagateChangesToAppliers() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = withMessage(createEventMessage());
        applyEvent(tx, event);

        checkEventReceived(entity, event);
    }

    @Test
    @DisplayName("create phase for applied event")
    void createPhaseForAppliedEvent() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = withMessage(createEventMessage());
        applyEvent(tx, event);

        assertThat(tx.phases())
                .hasSize(1);

        Phase<I, ?> phase = tx.phases()
                              .get(0);
        assertTrue(checkPhase(event, phase));
    }

    @Test
    @DisplayName("propagate changes to entity upon commit")
    void propagateChangesToEntityOnCommit() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = withMessage(createEventMessage());
        applyEvent(tx, event);
        S stateBeforeCommit = entity.state();
        Version versionBeforeCommit = entity.version();
        tx.commit();

        S modifiedState = entity.state();
        Version modifiedVersion = entity.version();

        assertThat(modifiedState)
                .isNotEqualTo(stateBeforeCommit);
        assertThat(modifiedVersion)
                .isNotEqualTo(versionBeforeCommit);
    }

    @Test
    @DisplayName("not propagate changes to entity on rollback")
    void notPropagateChangesOnRollback() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = withMessage(createEventMessage());
        applyEvent(tx, event);
        S stateBeforeRollback = entity.state();
        Version versionBeforeRollback = entity.version();
        tx.rollback(new RuntimeException("that triggers rollback"));

        S stateAfterRollback = entity.state();
        Version versionAfterRollback = entity.version();

        assertThat(stateAfterRollback)
                .isEqualTo(stateBeforeRollback);
        assertThat(versionAfterRollback)
                .isEqualTo(versionBeforeRollback);
    }

    @Test
    @DisplayName("set transaction entity version from event context")
    void setVersionFromEventContext() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);
        Event event = withMessage(createEventMessage());

        Version ctxVersion = event.context()
                                  .getVersion();
        assertThat(ctxVersion)
                .isNotEqualTo(tx.version());

        applyEvent(tx, event);
        Version modifiedVersion = tx.version();
        assertThat(tx.version())
                .isEqualTo(modifiedVersion);
    }

    @SuppressWarnings("unchecked")  // OK for a test method.
    @Test
    @DisplayName("notify listener during transaction execution")
    void notifyListenerDuringExecution() {
        TransactionListener<I, E, S, B> listener = mock(TransactionListener.class);
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTxWithListener(entity, listener);
        Event event = withMessage(createEventMessage());

        verifyZeroInteractions(listener);
        applyEvent(tx, event);

        verify(listener).onAfterPhase(argThat(matchesSuccessfulPhaseFor(event)));
    }

    @SuppressWarnings("CheckReturnValue") // can ignore new entity version in this test.
    @Test
    @DisplayName("not allow injecting state if entity has non-zero version already")
    void notInjectToEntityWithVersion() {
        E entity = createEntity();
        entity.incrementVersion();
        S newState = createNewState();
        Version newVersion = someVersion();

        assertThrows(IllegalStateException.class,
                     () -> createTxWithState(entity, newState, newVersion));
    }

    @Nested
    @DisplayName("propagate exception as `IllegalStateException`")
    class PropagatingExceptions {

        @Test
        @DisplayName("on phase failure")
        void onPhaseFailure() {
            E entity = createEntity();

            Transaction<I, E, S, B> tx = createTx(entity);

            Event event = withMessage(createEventThatFailsInHandler());

            assertThrows(IllegalStateException.class, () -> applyEvent(tx, event));
        }

        @Test
        @DisplayName("on commit failure")
        void onCommitFailure() {
            E entity = createEntity(someViolations());
            S newState = createNewState();
            Version version = someVersion();

            Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
            try {
                tx.commit();
                fail("Expected an exception due to a failed commit.");
            } catch (IllegalStateException e) {
                assertThat(e.getCause())
                        .isInstanceOf(InvalidEntityStateException.class);
            }
        }
    }

    @Nested
    @DisplayName("rollback automatically")
    class RollbackAutomatically {

        @Test
        @DisplayName("if phase failed")
        void ifPhaseFailed() {
            E entity = createEntity();
            S originalState = entity.state();
            Version originalVersion = entity.version();

            Transaction<I, E, S, B> tx = createTx(entity);

            Event event = withMessage(createEventThatFailsInHandler());
            try {
                applyEvent(tx, event);
                fail("Expected an `Exception` due to a failed phase execution.");
            } catch (Exception e) {
                checkRollback(entity, originalState, originalVersion);
            }
        }

        @Test
        @DisplayName("if commit failed")
        void ifCommitFailed() {
            E entity = createEntity(someViolations());
            S originalState = entity.state();
            Version originalVersion = entity.version();

            S newState = createNewState();
            Version version = someVersion();

            Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
            try {
                tx.commit();
                fail("Expected an IllegalStateException due to a failed commit.");
            } catch (IllegalStateException e) {
                checkRollback(entity, originalState, originalVersion);
            }
        }

        private void checkRollback(E entity, S originalState, Version originalVersion) {
            assertNull(entity.getTransaction());
            assertThat(entity.state())
                    .isEqualTo(originalState);
            assertThat(entity.version())
                    .isEqualTo(originalVersion);
        }
    }

    @Test
    @DisplayName("throw InvalidEntityStateException if constraints are violated on commit")
    void throwIfViolationOnCommit() {
        E entity = createEntity();

        S newState = createNewState();
        Version version = someVersion();

        Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        ValidationException toThrow = validationException();
        breakEntityValidation(toThrow);

        assertThrows(InvalidEntityStateException.class, tx::commit);
    }

    /**
     * Call this method in derived transaction tests if corresponding transaction
     * carries version number into an entity.
     *
     * @implNote This method uses package-private API of the {@link Transaction} class.
     */
    protected final void advanceVersionFromEvent() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);
        assertEquals(entity.version(), tx.version());

        Event event = withMessage(createEventMessage());
        applyEvent(tx, event);
        Version versionFromEvent = event.context()
                                        .getVersion();
        assertThat(tx.version())
                .isEqualTo(versionFromEvent);
        tx.commit();
        assertThat(entity.version())
                .isEqualTo(versionFromEvent);
    }

    private ArgumentMatcher<Phase<I, ?>> matchesSuccessfulPhaseFor(Event event) {
        return phase -> checkPhase(event, phase);
    }

    private static void breakEntityValidation(RuntimeException toThrow) {
        FakeOptionFactory.shouldFailWith(toThrow);
    }
}
