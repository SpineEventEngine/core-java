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
package org.spine3.server.entity;

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.test.TestEventFactory;
import org.spine3.validate.ValidatingBuilders.StringValueValidatingBuilder;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
public class EventPlayingEntityShould {

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventPlayingEntityShould.class);

    protected EventPlayingEntity getEntity() {
        return new EpeEntity(1L);
    }

    @Test
    public void be_non_changed_once_created() {
        assertFalse(getEntity().isChanged());
    }

    @Test
    public void become_changed_once_lifecycleFlags_are_updated() {
        final EventPlayingEntity entity = getEntity();
        entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                               .setDeleted(true)
                                               .build());
        assertTrue(entity.isChanged());
    }

    @Test
    public void have_null_transaction_by_default() {
        assertNull(getEntity().getTransaction());
    }

    @Test
    public void have_no_transaction_in_progress_by_default() {
        assertFalse(getEntity().isTransactionInProgress());
    }

    @Test
    @SuppressWarnings("unchecked")  // OK for the test.
    public void allow_injecting_transaction() {
        final EventPlayingEntity entity = getEntity();
        final Transaction tx = mock(Transaction.class);
        entity.injectTransaction(tx);

        assertEquals(tx, entity.getTransaction());
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
        getEntity().setArchived(true);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_archive_with_inactive_transaction() {
        final EventPlayingEntity entity = entityWithInactiveTx();

        entity.setArchived(true);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_delete_with_no_transaction() {
        getEntity().setDeleted(true);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_delete_with_inactive_transaction() {
        final EventPlayingEntity entity = entityWithInactiveTx();

        entity.setDeleted(true);
    }

    @Test(expected = IllegalStateException.class)
    public void require_active_transaction_to_play_events() {
        getEntity().play(newArrayList(eventFactory.createEvent(StringValue.getDefaultInstance())));
    }

    @SuppressWarnings("unchecked")  // OK for the test code.
    private EventPlayingEntity entityWithInactiveTx() {
        final EventPlayingEntity entity = getEntity();

        final Transaction tx = mock(Transaction.class);
        when(tx.isActive()).thenReturn(false);
        entity.injectTransaction(tx);
        return entity;
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private EventPlayingEntity entityWithActiveTx(boolean txChanged) {
        final EventPlayingEntity entity = getEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);

        entity.injectTransaction(tx);
        return entity;
    }

    private static class EpeEntity
            extends EventPlayingEntity<Long, StringValue, StringValueValidatingBuilder> {

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
