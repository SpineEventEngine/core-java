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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Version;
import io.spine.validate.StringValueVBuilder;
import io.spine.validate.ValidatingBuilder;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static io.spine.test.TestValues.newUuidValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
public class TransactionalEntityShould {

    protected TransactionalEntity newEntity() {
        return new TeEntity(1L);
    }

    @Test
    @DisplayName("have private ctor for TypeInfo")
    void havePrivateCtorForTypeInfo() {
        assertHasPrivateParameterlessCtor(TransactionalEntity.TypeInfo.class);
    }

    @Test
    @DisplayName("be non changed once created")
    void beNonChangedOnceCreated() {
        assertFalse(newEntity().isChanged());
    }

    @Test
    @DisplayName("become changed once lifecycleFlags are updated")
    void becomeChangedOnceLifecycleFlagsAreUpdated() {
        final TransactionalEntity entity = newEntity();
        entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                               .setDeleted(true)
                                               .build());
        assertTrue(entity.isChanged());
    }

    @Test
    @DisplayName("have null transaction by default")
    void haveNullTransactionByDefault() {
        assertNull(newEntity().getTransaction());
    }

    @Test
    @DisplayName("have no transaction in progress by default")
    void haveNoTransactionInProgressByDefault() {
        assertFalse(newEntity().isTransactionInProgress());
    }

    // todo messed up generation
    @Test
    @SuppressWarnings("unchecked")  // OK for the test.
    @DisplayName("ssWarnings")
    public void allow_injecting_transaction() {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);

        assertEquals(tx, entity.getTransaction());
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")  // OK for the test.
    @DisplayName("ssWarnings")
    public void disallow_injecting_transaction_wrapped_around_another_entity_instance() {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(newEntity());
        entity.injectTransaction(tx);
    }

    @Test
    @DisplayName("have no transaction in progress until tx started")
    void haveNoTransactionInProgressUntilTxStarted() {
        final TransactionalEntity entity = entityWithInactiveTx();

        assertFalse(entity.isTransactionInProgress());
    }

    @Test
    @DisplayName("have transaction in progress when tx is active")
    void haveTransactionInProgressWhenTxIsActive() {
        final TransactionalEntity entity = entityWithActiveTx(false);

        assertTrue(entity.isTransactionInProgress());
    }

    @Test
    @DisplayName("be non changed if transaction isnt changed")
    void beNonChangedIfTransactionIsntChanged() {
        final TransactionalEntity entity = entityWithActiveTx(false);

        assertFalse(entity.isChanged());
    }

    @Test
    @DisplayName("become changed if transaction state changed")
    void becomeChangedIfTransactionStateChanged() {
        final TransactionalEntity entity = entityWithActiveTx(true);

        assertTrue(entity.isChanged());
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("fail to archive with no transaction")
    void failToArchiveWithNoTransaction() {
        newEntity().setArchived(true);
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("fail to archive with inactive transaction")
    void failToArchiveWithInactiveTransaction() {
        final TransactionalEntity entity = entityWithInactiveTx();

        entity.setArchived(true);
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("fail to delete with no transaction")
    void failToDeleteWithNoTransaction() {
        newEntity().setDeleted(true);
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("fail to delete with inactive transaction")
    void failToDeleteWithInactiveTransaction() {
        final TransactionalEntity entity = entityWithInactiveTx();

        entity.setDeleted(true);
    }

    @Test
    @DisplayName("return tx lifecycleFlags if tx is active")
    void returnTxLifecycleFlagsIfTxIsActive() {
        final TransactionalEntity entity = entityWithInactiveTx();
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
    @DisplayName("return non null builder from state")
    void returnNonNullBuilderFromState() {
        final ValidatingBuilder builder = newEntity().builderFromState();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("return builder reflecting current state")
    void returnBuilderReflectingCurrentState() {
        final TransactionalEntity entity = newEntity();
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
    private TransactionalEntity entityWithInactiveTx() {
        final TransactionalEntity entity = newEntity();

        final Transaction tx = mock(Transaction.class);
        when(tx.isActive()).thenReturn(false);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);
        return entity;
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private TransactionalEntity entityWithActiveTx(boolean txChanged) {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = spy(mock(Transaction.class));
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);
        when(tx.getEntity()).thenReturn(entity);

        entity.injectTransaction(tx);
        return entity;
    }

    private static class TeEntity
            extends TransactionalEntity<Long, StringValue, StringValueVBuilder> {

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the
         *                                  {@linkplain Entity supported types}
         */
        private TeEntity(Long id) {
            super(id);
        }
    }
}
