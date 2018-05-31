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
import io.spine.core.Events;
import io.spine.core.Version;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.Transaction.Phase;
import io.spine.server.event.EventFactory;
import io.spine.server.model.ModelTests;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidatingBuilder;
import io.spine.validate.ValidationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Versions.newVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
public abstract class TransactionShould<I,
        E extends TransactionalEntity<I, S, B>,
        S extends Message,
        B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    private final EventFactory eventFactory = TestEventFactory.newInstance(TransactionShould.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    private static boolean checkPhase(Event event, Phase phase, boolean successful) {
        Message eventMessage = Events.getMessage(event);
        return eventMessage.equals(phase.getEventMessage())
                && event.getContext()
                        .equals(phase.getContext())
                && phase.isSuccessful() == successful;
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

    @Before
    public void setUp() {
        ModelTests.clearModel();
    }

    @Test
    public void initialize_from_entity() {
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
    public void propagate_changes_to_entity_applier_methods() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessage());
        applyEvent(tx, event);

        checkEventReceived(entity, event);
    }

    @Test
    public void create_phase_for_an_applied_event() {
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessage());
        applyEvent(tx, event);

        assertEquals(1, tx.getPhases()
                          .size());
        Phase<I, E, S, B> phase = tx.getPhases()
                                    .get(0);
        assertTrue(checkPhase(event, phase, true));
    }

    @Test
    public void propagate_changes_to_entity_upon_commit() {
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
    public void not_propagate_changes_to_entity_on_rollback() {
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
    public void set_txEntityVersion_from_EventContext() {
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

    @Test
    @SuppressWarnings({"unchecked", "ConstantConditions"})  // OK for a test method.
    public void notify_listener_during_transaction_execution() {
        TransactionListener<I, E, S, B> listener = mock(TransactionListener.class);
        E entity = createEntity();
        Transaction<I, E, S, B> tx = createTxWithListener(entity, listener);
        Event event = createEvent(createEventMessage());

        verifyZeroInteractions(listener);
        applyEvent(tx, event);

        verify(listener).onAfterPhase(argThat(matchesSuccessfulPhaseFor(event)));
    }

    @Test
    public void init_state_and_version_if_specified_in_ctor() {
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
    public void not_allow_injecting_state_if_entity_has_non_zero_version_already() {
        E entity = createEntity();
        entity.incrementVersion();
        S newState = createNewState();
        Version newVersion = someVersion();

        thrown.expect(IllegalStateException.class);
        createTxWithState(entity, newState, newVersion);
    }

    @Test
    public void propagate_phase_failure_exception_as_ISE() {
        E entity = createEntity();

        Transaction<I, E, S, B> tx = createTx(entity);

        Event event = createEvent(createEventMessageThatFailsInHandler());

        thrown.expect(IllegalStateException.class);
        applyEvent(tx, event);
    }

    @Test
    public void rollback_automatically_if_phase_failed() {
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
    public void propagate_exception_wrapped_as_ISE_if_commit_fails() {
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
    public void throw_InvalidEntityStateException_if_constraints_violated_on_commit() {
        E entity = createEntity();

        S newState = createNewState();
        Version version = someVersion();

        Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        ValidationException toThrow = validationException();
        breakEntityValidation(entity, toThrow);

        thrown.expect(InvalidEntityStateException.class);
        tx.commit();
    }

    @Test
    public void rollback_automatically_if_commit_failed() {
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
    public void advance_version_from_event() {
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
        return phase -> checkPhase(event, phase, true);
    }

    private void applyEvent(Transaction<I, E, S, B> tx, Event event) {
        tx.apply(EventEnvelope.of(event));
    }
}
