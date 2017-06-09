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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Event;
import io.spine.base.Version;
import io.spine.test.TestEventFactory;
import io.spine.test.Values;
import io.spine.validate.StringValueVBuilder;
import io.spine.validate.ValidatingBuilder;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Values.newUuidValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
public class EventPlayingEntityShould {

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventPlayingEntityShould.class);

    protected EventPlayingEntity newEntity() {
        return new EpeEntity(1L);
    }

    @Test
    public void have_private_ctor_for_TypeInfo() {
        assertHasPrivateParameterlessCtor(EventPlayingEntity.TypeInfo.class);
    }

    @Test
    public void be_non_changed_once_created() {
        assertFalse(newEntity().isChanged());
    }

    @Test
    public void become_changed_once_lifecycleFlags_are_updated() {
        final EventPlayingEntity entity = newEntity();
        entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                               .setDeleted(true)
                                               .build());
        assertTrue(entity.isChanged());
    }

    @Test
    public void have_null_transaction_by_default() {
        assertNull(newEntity().getTransaction());
    }

    @Test
    public void have_no_transaction_in_progress_by_default() {
        assertFalse(newEntity().isTransactionInProgress());
    }

    @Test
    @SuppressWarnings("unchecked")  // OK for the test.
    public void allow_injecting_transaction() {
        final EventPlayingEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);

        assertEquals(tx, entity.getTransaction());
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")  // OK for the test.
    public void disallow_injecting_transaction_wrapped_around_another_entity_instance() {
        final EventPlayingEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(newEntity());
        entity.injectTransaction(tx);
    }

    @Test
    public void have_no_transaction_in_progress_until_tx_started() {
        final EventPlayingEntity entity = entityWithInactiveTx();

        assertFalse(entity.isTransactionInProgress());
    }

    @Test
    public void have_transaction_in_progress_when_tx_is_active() {
        final EventPlayingEntity entity = entityWithActiveTx(false);

        assertTrue(entity.isTransactionInProgress());
    }

    @Test
    public void be_non_changed_if_transaction_isnt_changed() {
        final EventPlayingEntity entity = entityWithActiveTx(false);

        assertFalse(entity.isChanged());
    }

    @Test
    public void become_changed_if_transaction_state_changed() {
        final EventPlayingEntity entity = entityWithActiveTx(true);

        assertTrue(entity.isChanged());
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_archive_with_no_transaction() {
        newEntity().setArchived(true);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_archive_with_inactive_transaction() {
        final EventPlayingEntity entity = entityWithInactiveTx();

        entity.setArchived(true);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_delete_with_no_transaction() {
        newEntity().setDeleted(true);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_delete_with_inactive_transaction() {
        final EventPlayingEntity entity = entityWithInactiveTx();

        entity.setDeleted(true);
    }

    @Test(expected = IllegalStateException.class)
    public void require_active_transaction_to_play_events() {
        newEntity().play(newArrayList(eventFactory.createEvent(StringValue.getDefaultInstance())));
    }

    @Test
    public void delegate_applying_events_to_tx_when_playing() {
        final EventPlayingEntity entity = entityWithActiveTx(false);
        final Transaction txMock = entity.getTransaction();
        assertNotNull(txMock);
        final Event firstEvent = eventFactory.createEvent(Values.newUuidValue());
        final Event secondEvent = eventFactory.createEvent(Values.newUuidValue());

        entity.play(newArrayList(firstEvent, secondEvent));

        verifyEventApplied(txMock, firstEvent);
        verifyEventApplied(txMock, secondEvent);
    }

    private static void verifyEventApplied(Transaction txMock, Event event) {
        final Message actualMessage = unpack(event.getMessage());
        verify(txMock).apply(eq(actualMessage),
                             eq(event.getContext()));
    }

    @Test
    public void return_tx_lifecycleFlags_if_tx_is_active() {
        final EventPlayingEntity entity = entityWithInactiveTx();
        final LifecycleFlags originalFlags = entity.getLifecycleFlags();

        final LifecycleFlags modifiedFlags = originalFlags.toBuilder()
                                                          .setDeleted(true)
                                                          .build();

        assertNotEquals(originalFlags, modifiedFlags);

        final Transaction txMock = entity.getTransaction();
        assertNotNull(txMock);
        when(txMock.isActive()).thenReturn(true);
        when(txMock.getLifecycleFlags()).thenReturn(modifiedFlags);

        final LifecycleFlags actual = entity.getLifecycleFlags();
        assertEquals(modifiedFlags, actual);
    }

    @Test
    public void return_non_null_builder_from_state() {
        final ValidatingBuilder builder = newEntity().builderFromState();
        assertNotNull(builder);
    }

    @Test
    public void return_builder_reflecting_current_state() {
        final EventPlayingEntity entity = newEntity();
        final Message originalState = entity.builderFromState()
                                            .build();

        final StringValue newState = newUuidValue();
        assertNotEquals(originalState, newState);

        TestTransaction.injectState(entity, newState, Version.getDefaultInstance());
        final Message modifiedState = entity.builderFromState()
                                            .build();

        assertEquals(newState, modifiedState);
    }

    @SuppressWarnings("unchecked")  // OK for the test code.
    private EventPlayingEntity entityWithInactiveTx() {
        final EventPlayingEntity entity = newEntity();

        final Transaction tx = mock(Transaction.class);
        when(tx.isActive()).thenReturn(false);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);
        return entity;
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private EventPlayingEntity entityWithActiveTx(boolean txChanged) {
        final EventPlayingEntity entity = newEntity();
        final Transaction tx = spy(mock(Transaction.class));
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);
        when(tx.getEntity()).thenReturn(entity);

        entity.injectTransaction(tx);
        return entity;
    }

    private static class EpeEntity
            extends EventPlayingEntity<Long, StringValue, StringValueVBuilder> {

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the
         *                                  {@linkplain Entity supported types}
         */
        private EpeEntity(Long id) {
            super(id);
        }
    }
}
