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
package io.spine.server.entity;

import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.Transaction.Phase;
import io.spine.server.event.EventFactory;
import io.spine.server.model.ModelTests;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidatingBuilder;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Events.getMessage;
import static io.spine.core.Versions.newVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Base class for testing the {@linkplain Transaction transactions} for different
 * {@linkplain TransactionalEntity TransactionalEntity} implementations.
 *
 * @author Alex Tymchenko
 */
public abstract class TransactionTest<I,
        E extends TransactionalEntity<I, S, B>,
        S extends Message,
        B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    private final EventFactory eventFactory = TestEventFactory.newInstance(TransactionTest.class);

    private static ValidationException validationException() {
        ValidationException ex = new ValidationException(Lists.newLinkedList());
        return ex;
    }

    private static List<ConstraintViolation> someViolations() {
        ConstraintViolation expectedViolation = ConstraintViolation
                .newBuilder()
                .setMsgFormat("Some violation %s")
                .addParam("1")
                .build();
        return newArrayList(expectedViolation);
    }

    private static boolean checkPhase(Event event, Phase phase) {
        Message eventMessage = getMessage(event);
        boolean equalMessages = eventMessage.equals(phase.getEventMessage());
        boolean equalContexts = event.getContext()
                                     .equals(phase.getContext());
        boolean isSuccessful = phase.isSuccessful();
        return equalMessages
                && equalContexts
                && isSuccessful;
    }

    private static Version someVersion() {
        return newVersion(42, getCurrentTime());
    }

    protected abstract Transaction<I, E, S, B> createTx(E entity);

    protected abstract Transaction<I, E, S, B> createTxWithState(E entity,
                                                                 S state,
                                                                 Version version);

    protected abstract Transaction<I, E, S, B>
    createTxWithListener(E entity, TransactionListener<I, E, S, B> listener);

    protected abstract E createEntity();

    protected abstract E createEntity(List<ConstraintViolation> violations);

    protected abstract S createNewState();

    protected abstract void checkEventReceived(E entity, Event event);

    protected abstract Message createEventMessage();

    protected abstract Message createEventMessageThatFailsInHandler();

    protected abstract void breakEntityValidation(E entity, RuntimeException toThrow);

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
    }

    @Test
    @DisplayName("initialize from entity")
    void initializeFromEntity() {
        E entity = createEntity();
        B expectedBuilder = entity.builderFromState();
        Version expectedVersion = entity.getVersion();
        LifecycleFlags expectedLifecycleFlags = entity.getLifecycleFlags();

        Transaction<I, E, S, B> tx = createTx(entity);
        assertNotNull(tx);

        assertEquals(entity, tx.getEntity());
        // Not possible to compare `Message.Builder` instances via `equals`, so trigger `build()`.
        assertEquals(expectedBuilder.build(), tx.getBuilder()
                                                .build());
        assertEquals(expectedVersion, tx.getVersion());
        assertEquals(expectedLifecycleFlags, tx.getLifecycleFlags());

        assertTrue(tx.isActive());

        assertTrue(tx.getPhases()
                     .isEmpty());
    }

    @Test
    @DisplayName("propagate changes to entity applier methods")
    void propagateChangesToApplierMethods() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessage());
        applyEvent(tx, event);

        checkEventReceived(entity, event);
    }

    @Test
    @DisplayName("create phase for applied event")
    void createPhaseForAppliedEvent() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessage());
        applyEvent(tx, event);

        assertEquals(1, tx.getPhases()
                          .size());
        Phase<I, E, S, B> phase = tx.getPhases()
                                    .get(0);
        assertTrue(checkPhase(event, phase));
    }

    @Test
    @DisplayName("propagate changes to entity upon commit")
    void propagateChangesToEntityUponCommit() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessage());
        applyEvent(tx, event);
        S stateBeforeCommit = entity.getState();
        Version versionBeforeCommit = entity.getVersion();
        tx.commit();

        S modifiedState = entity.getState();
        Version modifiedVersion = entity.getVersion();
        assertNotEquals(stateBeforeCommit, modifiedState);
        assertNotEquals(versionBeforeCommit, modifiedVersion);
    }

    @Test
    @DisplayName("not propagate changes to entity on rollback")
    void notPropagateChangesOnRollback() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessage());
        applyEvent(tx, event);
        S stateBeforeRollback = entity.getState();
        Version versionBeforeRollback = entity.getVersion();
        tx.rollback(new RuntimeException("that triggers rollback"));

        S stateAfterRollback = entity.getState();
        Version versionAfterRollback = entity.getVersion();
        assertEquals(stateBeforeRollback, stateAfterRollback);
        assertEquals(versionBeforeRollback, versionAfterRollback);
    }

    @Test
    @DisplayName("set `txEntityVersion` from EventContext")
    void setTxEntityVersionFromEventContext() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);
        Event event = createEvent(createEventMessage());

        Version ctxVersion = event.getContext()
                                  .getVersion();
        assertNotEquals(tx.getVersion(), ctxVersion);

        applyEvent(tx, event);
        Version modifiedVersion = tx.getVersion();
        assertEquals(modifiedVersion, tx.getVersion());
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})  // OK for a test method.
    @Test
    @DisplayName("notify listener during transaction execution")
    void notifyListenerDuringTransactionExecution() {
        TransactionListener<I, E, S, B> listener = mock(TransactionListener.class);
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTxWithListener(entity, listener);
        Event event = createEvent(createEventMessage());

        verifyZeroInteractions(listener);
        applyEvent(tx, event);

        verify(listener).onAfterPhase(argThat(matchesSuccessfulPhaseFor(event)));
    }

    @Test
    @DisplayName("init state and version if specified in constructor")
    void initStateAndVersionIfSpecifiedInCtor() {
        E entity = createEntity();
        S newState = createNewState();
        Version newVersion = someVersion();

        assertNotEquals(entity.getState(), newState);
        assertNotEquals(entity.getVersion(), newVersion);

        Transaction<I, E, S, B> tx = createTxWithState(entity, newState, newVersion);

        assertEquals(newState, tx.getBuilder()
                                 .build());
        assertEquals(newVersion, tx.getVersion());
        assertNotEquals(entity.getState(), newState);
        assertNotEquals(entity.getVersion(), newVersion);

        tx.commit();

        assertEquals(entity.getState(), newState);
        assertEquals(entity.getVersion(), newVersion);
    }

    @SuppressWarnings("CheckReturnValue") // can ignore new entity version in this test.
    @Test
    @DisplayName("not allow injecting state if entity has non-zero version already")
    void notAllowInjectingStateIfEntityHasNonZeroVersionAlready() {
        E entity = createEntity();
        entity.incrementVersion();
        S newState = createNewState();
        Version newVersion = someVersion();

        assertThrows(IllegalStateException.class,
                     () -> createTxWithState(entity, newState, newVersion));
    }

    @Test
    @DisplayName("propagate phase failure exception as ISE")
    void propagatePhaseFailureExceptionAsISE() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessageThatFailsInHandler());

        assertThrows(IllegalStateException.class, () -> applyEvent(tx, event));
    }

    @Test
    @DisplayName("rollback automatically if phase failed")
    void rollbackAutomaticallyIfPhaseFailed() {
        E entity = createEntity();
        S originalState = entity.getState();
        Version originalVersion = entity.getVersion();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessageThatFailsInHandler());
        try {
            applyEvent(tx, event);
            fail("Expected an `Exception` due to a failed phase execution.");
        } catch (Exception e) {
            checkRollback(entity, originalState, originalVersion);
        }
    }

    @Test
    @DisplayName("propagate exception wrapped as ISE if commit fails")
    void propagateExceptionWrappedAsISEIfCommitFails() {
        E entity = createEntity(someViolations());
        S newState = createNewState();
        Version version = someVersion();

        Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        try {
            tx.commit();
            fail("Expected an exception due to a failed commit.");
        } catch (IllegalStateException e) {
            assertEquals(InvalidEntityStateException.class, e.getCause()
                                                             .getClass());
        }
    }

    @Test
    @DisplayName("throw InvalidEntityStateException if constraints violated on commit")
    void throwInvalidEntityStateExceptionIfConstraintsViolatedOnCommit() {
        E entity = createEntity();

        S newState = createNewState();
        Version version = someVersion();

        Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        ValidationException toThrow = validationException();
        breakEntityValidation(entity, toThrow);

        assertThrows(InvalidEntityStateException.class, tx::commit);
    }

    @Test
    @DisplayName("rollback automatically if commit failed")
    void rollbackAutomaticallyIfCommitFailed() {
        E entity = createEntity(someViolations());
        S originalState = entity.getState();
        Version originalVersion = entity.getVersion();

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

    @Test
    @DisplayName("advance version from event")
    protected void advanceVersionFromEvent() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);
        assertEquals(entity.getVersion(), tx.getVersion());

        Event event = createEvent(createEventMessage());
        EventEnvelope envelope = EventEnvelope.of(event);
        tx.apply(envelope);

        assertEquals(event.getContext()
                          .getVersion(), tx.getVersion());
        tx.commit();
        assertEquals(event.getContext()
                          .getVersion(), entity.getVersion());
    }

    protected final Event createEvent(Message eventMessage) {
        return eventFactory.createEvent(eventMessage,
                                        someVersion());
    }

    private void checkRollback(E entity, S originalState, Version originalVersion) {
        assertNull(entity.getTransaction());
        assertEquals(originalState, entity.getState());
        assertEquals(originalVersion, entity.getVersion());
    }

    private ArgumentMatcher<Phase<I, E, S, B>> matchesSuccessfulPhaseFor(Event event) {
        return phase -> checkPhase(event, phase);
    }

    private void applyEvent(Transaction<I, E, S, B> tx, Event event) {
        tx.apply(EventEnvelope.of(event));
    }
}
