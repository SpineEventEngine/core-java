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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.given.tx.Id;
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.entity.given.tx.event.TxStateErrorRequested;
import io.spine.test.validation.FakeOptionFactory;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.server.entity.Transaction.toBuilder;
import static io.spine.server.type.given.GivenEvent.withMessage;
import static io.spine.server.type.given.GivenEvent.withMessageAndVersion;
import static io.spine.testing.TestValues.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Base class for testing the {@linkplain Transaction transactions} for different
 * {@link TransactionalEntity} implementations.
 */
public abstract class TransactionTest<I,
        E extends TransactionalEntity<I, S, B>,
        S extends Message,
        B extends ValidatingBuilder<S>> {

    /**
     * Creates the instance of the ID with the simple name of test suite class.
     */
    protected final Id id() {
        return Id.newBuilder()
                 .setId(getClass().getSimpleName())
                 .build();
    }

    private static boolean checkPhase(Event event, Phase phase) {
        EventId id = event.getId();
        Message phaseId = phase.messageId();
        boolean equalIds = id.equals(phaseId);
        boolean isSuccessful = phase.isSuccessful();
        return equalIds && isSuccessful;
    }

    private static Version newVersion() {
        return Versions.newVersion(42, currentTime());
    }

    protected abstract Transaction<I, E, S, B> createTx(E entity);

    protected abstract Transaction<I, E, S, B> createTx(E entity, S state, Version version);

    protected abstract
    Transaction<I, E, S, B> createTx(E entity, TransactionListener<I> listener);

    protected abstract E createEntity();

    protected abstract S newState();

    protected abstract void checkEventReceived(E entity, Event event);

    /**
     * Creates an event message which would be normally handled by the entity.
     */
    protected final EventMessage createEventMessage() {
        return TxCreated.newBuilder()
                        .setId(id())
                        .setName("Name " + randomString())
                        .build();
    }

    /**
     * Creates an event message handling of which causes an exception in the entity
     * served by the transaction under the test.
     */
    protected final EventMessage failingInHandler() {
        return TxErrorRequested
                .newBuilder()
                .setId(id())
                .build();
    }

    /**
     * Creates an event message handling of which turns the builder of the entity state
     * into the state which fails the validation.
     */
    protected final EventMessage failingStateTransition() {
        return TxStateErrorRequested
                .newBuilder()
                .setId(id())
                .build();
    }

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
            B expectedBuilder = toBuilder(entity);
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
            S newState = newState();
            Version newVersion = newVersion();

            assertThat(newState)
                    .isNotEqualTo(entity.state());
            assertThat(newVersion)
                    .isNotEqualTo(entity.version());

            Transaction<I, E, S, B> tx = createTx(entity, newState, newVersion);

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
        TransactionListener<I> listener = mock(TransactionListener.class);
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity, listener);
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
        S newState = newState();
        Version newVersion = newVersion();

        assertThrows(IllegalStateException.class,
                     () -> createTx(entity, newState, newVersion));
    }

    @Nested
    @DisplayName("throw")
    class ThrowExceptions {

        @Test
        @DisplayName("`IllegalStateException` on phase failure")
        void onPhaseFailure() {
            E entity = createEntity();

            Transaction<I, E, S, B> tx = createTx(entity);

            Event event = withMessage(failingInHandler());

            assertThrows(IllegalStateException.class, () -> applyEvent(tx, event));
        }

        @Test
        @DisplayName("`InvalidEntityStateException` on state transition failure")
        void onCommitFailure() {
            E entity = createEntity();
            Transaction<I, E, S, B> tx = createTx(entity);
            Event event = withMessage(failingStateTransition());
            applyEvent(tx, event);

            assertThrows(InvalidEntityStateException.class, tx::commit);
        }
    }

    @Nested
    @DisplayName("rollback automatically")
    class RollbackAutomatically {

        @Test
        @DisplayName("on violation at phase")
        void ifPhaseFailed() {
            E entity = createEntity();
            S originalState = entity.state();
            Version originalVersion = entity.version();

            Transaction<I, E, S, B> tx = createTx(entity);

            Event event = withMessage(failingInHandler());
            assertThrows(IllegalStateException.class, () -> applyEvent(tx, event));
            checkRollback(entity, originalState, originalVersion);
        }

        @Test
        @DisplayName("on violation at commit")
        void ifCommitFailed() {
            E entity = createAndModify();
            S originalState = entity.state();
            Version originalVersion = entity.version();

            Transaction<I, E, S, B> tx = createTx(entity);
            Version nextVersion = Versions.increment(entity.version());
            Event event = withMessageAndVersion(failingStateTransition(), nextVersion);
            applyEvent(tx, event);

            //TODO:2019-06-16:alex.tymchenko: do we need to throw InvalidEntityStateException?
            //assertThrows(InvalidEntityStateException.class, tx::commit);
            tx.commit();
            checkRollback(entity, originalState, originalVersion);
        }

        private E createAndModify() {
            E entity = createEntity();
            Transaction<I, E, S, B> tx = createTx(entity);
            Event event = withMessage(createEventMessage());
            applyEvent(tx, event);
            tx.commit();
            return entity;
        }

        private void checkRollback(E entity, S originalState, Version originalVersion) {
            assertNull(entity.transaction());
            assertThat(entity.state())
                    .isEqualTo(originalState);
            assertThat(entity.version())
                    .isEqualTo(originalVersion);
        }
    }

    @Test
    @DisplayName("init builder with the entity ID, if the field is required or assumed required")
    void initBuilderWithId() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);
        FieldDescriptor firstField =
                entity.state()
                      .getDescriptorForType()
                      .getFields()
                      .get(0);
        assertThat(tx.builder()
                     .getField(firstField))
                .isEqualTo(entity.id());
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
}
