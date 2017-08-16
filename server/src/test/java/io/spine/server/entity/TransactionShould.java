/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.Versions.newVersion;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.time.Time.getCurrentTime;
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
 * {@linkplain EventPlayingEntity EventPlayingEntity} implementations.
 *
 * @author Alex Tymchenko
 */
public abstract class TransactionShould<I,
        E extends EventPlayingEntity<I, S, B>,
        S extends Message,
        B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    private final EventFactory eventFactory = TestEventFactory.newInstance(TransactionShould.class);

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
        final E entity = createEntity();
        final B expectedBuilder = entity.builderFromState();
        final Version expectedVersion = entity.getVersion();
        final LifecycleFlags expectedLifecycleFlags = entity.getLifecycleFlags();

        final Transaction<I, E, S, B> tx = createTx(entity);
        assertNotNull(tx);

        assertEquals(entity, tx.getEntity());
        // Not possible to compare `Message.Builder` instances via `equals`, so trigger `build()`.
        assertEquals(expectedBuilder.build(), tx.getBuilder().build());
        assertEquals(expectedVersion, tx.getVersion());
        assertEquals(expectedLifecycleFlags, tx.getLifecycleFlags());

        assertTrue(tx.isActive());

        assertTrue(tx.getPhases().isEmpty());
    }

    @Test
    public void propagate_changes_to_entity_applier_methods() {
        final E entity = createEntity();
        final Transaction<I, E, S, B> tx = createTx(entity);

        final Event event = createEvent(createEventMessage());
        applyEvent(tx, event);

        checkEventReceived(entity, event);
    }

    @Test
    public void create_phase_for_an_applied_event() {
        final E entity = createEntity();
        final Transaction<I, E, S, B> tx = createTx(entity);

        final Event event = createEvent(createEventMessage());
        applyEvent(tx, event);

        assertEquals(1, tx.getPhases().size());
        final Phase<I, E, S, B> phase = tx.getPhases()
                                          .get(0);
        assertTrue(checkPhase(event, phase, true));
    }

    @Test
    public void propagate_changes_to_entity_upon_commit() {
        final E entity = createEntity();

        final Transaction<I, E, S, B> tx = createTx(entity);

        final Event event = createEvent(createEventMessage());
        applyEvent(tx, event);
        final S stateBeforeCommit = entity.getState();
        final Version versionBeforeCommit = entity.getVersion();
        tx.commit();

        final S modifiedState = entity.getState();
        final Version modifiedVersion = entity.getVersion();
        assertNotEquals(stateBeforeCommit, modifiedState);
        assertNotEquals(versionBeforeCommit, modifiedVersion);
    }

    @Test
    public void not_propagate_changes_to_entity_on_rollback() {
        final E entity = createEntity();

        final Transaction<I, E, S, B> tx = createTx(entity);

        final Event event = createEvent(createEventMessage());
        applyEvent(tx, event);
        final S stateBeforeRollback = entity.getState();
        final Version versionBeforeRollback = entity.getVersion();
        tx.rollback(new RuntimeException("that triggers rollback"));

        final S stateAfterRollback = entity.getState();
        final Version versionAfterRollback = entity.getVersion();
        assertEquals(stateBeforeRollback, stateAfterRollback);
        assertEquals(versionBeforeRollback, versionAfterRollback);
    }

    @Test
    public void set_txEntityVersion_from_EventContext() {
        final E entity = createEntity();

        final Transaction<I, E, S, B> tx = createTx(entity);
        final Event event = createEvent(createEventMessage());

        final Version ctxVersion = event.getContext()
                                        .getVersion();
        assertNotEquals(tx.getVersion(), ctxVersion);

        applyEvent(tx, event);
        final Version modifiedVersion = tx.getVersion();
        assertEquals(modifiedVersion, tx.getVersion());
    }

    @Test
    @SuppressWarnings({"unchecked", "ConstantConditions"})  // OK for a test method.
    public void notify_listener_during_transaction_execution() {
        final TransactionListener<I, E, S ,B> listener = mock(TransactionListener.class);
        final E entity = createEntity();
        final Transaction<I, E, S ,B> tx = createTxWithListener(entity, listener);
        final Event event = createEvent(createEventMessage());

        verifyZeroInteractions(listener);
        applyEvent(tx, event);

        verify(listener).onAfterPhase(argThat(matchesSuccessfulPhaseFor(event)));
    }

    @Test
    public void init_state_and_version_if_specified_in_ctor(){
        final E entity = createEntity();
        final S newState = createNewState();
        final Version newVersion = someVersion();

        assertNotEquals(entity.getState(), newState);
        assertNotEquals(entity.getVersion(), newVersion);

        final Transaction<I, E, S, B> tx = createTxWithState(entity, newState, newVersion);

        assertEquals(newState, tx.getBuilder().build());
        assertEquals(newVersion, tx.getVersion());
        assertNotEquals(entity.getState(), newState);
        assertNotEquals(entity.getVersion(), newVersion);

        tx.commit();

        assertEquals(entity.getState(), newState);
        assertEquals(entity.getVersion(), newVersion);
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_injecting_state_if_entity_has_non_zero_version_already(){
        final E entity = createEntity();
        entity.incrementVersion();
        final S newState = createNewState();
        final Version newVersion = someVersion();

        createTxWithState(entity, newState, newVersion);
    }

    @Test(expected = IllegalStateException.class)
    public void propagate_phase_failure_exception_as_ISE() {
        final E entity = createEntity();

        final Transaction<I, E, S, B> tx = createTx(entity);

        final Event event = createEvent(createEventMessageThatFailsInHandler());
        applyEvent(tx, event);
    }

    @Test
    public void rollback_automatically_if_phase_failed() {
        final E entity = createEntity();
        final S originalState = entity.getState();
        final Version originalVersion = entity.getVersion();

        final Transaction<I, E, S, B> tx = createTx(entity);

        final Event event = createEvent(createEventMessageThatFailsInHandler());
        try {
            applyEvent(tx, event);
            fail("Expected an `Exception` due to a failed phase execution.");
        } catch (final Exception e) {
            checkRollback(entity, originalState, originalVersion);
        }
    }

    @Test
    public void propagate_exception_wrapped_as_ISE_if_commit_fails() {
        final E entity = createEntity(someViolations());
        final S newState = createNewState();
        final Version version = someVersion();

        final Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        try {
            tx.commit();
            fail("Expected an exception due to a failed commit.");
        } catch (IllegalStateException e) {
            assertEquals(InvalidEntityStateException.class, e.getCause().getClass());
        }
    }

    @Test(expected = InvalidEntityStateException.class)
    public void throw_InvalidEntityStateException_if_constraints_violated_on_commit() {
        final E entity = createEntity();

        final S newState = createNewState();
        final Version version = someVersion();

        final Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        final ValidationException toThrow = validationException();
        breakEntityValidation(entity, toThrow);
        tx.commit();
    }

    @Test
    public void rollback_automatically_if_commit_failed() {
        final E entity = createEntity(someViolations());
        final S originalState = entity.getState();
        final Version originalVersion = entity.getVersion();

        final S newState = createNewState();
        final Version version = someVersion();

        final Transaction<I, E, S, B> tx = createTxWithState(entity, newState, version);
        try {
            tx.commit();
            fail("Expected an IllegalStateException due to a failed commit.");
        } catch (IllegalStateException e) {
            checkRollback(entity, originalState, originalVersion);
        }
    }

    private static ValidationException validationException() {
        final ValidationException ex = new ValidationException(Lists.<ConstraintViolation>newLinkedList());
        return ex;
    }

    private void checkRollback(E entity, S originalState, Version originalVersion) {
        assertNull(entity.getTransaction());
        assertEquals(originalState, entity.getState());
        assertEquals(originalVersion, entity.getVersion());
    }

    private static List<ConstraintViolation> someViolations() {
        final ConstraintViolation expectedViolation =
                ConstraintViolation.newBuilder()
                                   .setMsgFormat("Some violation %s")
                                   .addParam("1")
                                   .build();
        return newArrayList(expectedViolation);
    }

    private ArgumentMatcher<Phase<I, E, S, B>> matchesSuccessfulPhaseFor(final Event event) {
        return new ArgumentMatcher<Phase<I, E, S, B>>() {
            @Override
            public boolean matches(Phase<I, E, S, B> phase) {
                return checkPhase(event, phase, true);
            }
        };
    }

    private static boolean checkPhase(Event event, Phase phase, boolean successful) {
        return unpack(event.getMessage()).equals(phase.getEventMessage())
                                  && event.getContext().equals(phase.getContext())
                && phase.isSuccessful() == successful;
    }


    private void applyEvent(Transaction<I, E, S, B> tx, Event event) {
        tx.apply(EventEnvelope.of(event));
    }

    private Event createEvent(Message eventMessage) {
        return eventFactory.createEvent(eventMessage,
                                        someVersion());
    }

    private static Version someVersion() {
        return newVersion(1, getCurrentTime());
    }

}
