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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.test.TestValues.newUuidValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
@DisplayName("TransactionalEntity should")
class TransactionalEntityTest {

    protected TransactionalEntity newEntity() {
        return new TeEntity(1L);
    }

    @Test
    @DisplayName("have private constructor for TypeInfo")
    void havePrivateTypeInfoCtor() {
        assertHasPrivateParameterlessCtor(TransactionalEntity.TypeInfo.class);
    }

    @Test
    @DisplayName("be non-changed once created")
    void beNonChangedOnceCreated() {
        assertFalse(newEntity().isChanged());
    }

    @Test
    @DisplayName("become changed once `lifecycleFlags` are updated")
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

    @SuppressWarnings("unchecked")  // OK for the test.
    @Test
    @DisplayName("allow injecting transaction")
    void allowInjectingTransaction() {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);

        assertEquals(tx, entity.getTransaction());
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    @Test
    @DisplayName("disallow injecting transaction wrapped around another entity instance")
    void disallowOtherInstanceTransaction() {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(newEntity());
        assertThrows(IllegalStateException.class, () -> entity.injectTransaction(tx));
    }

    @Test
    @DisplayName("have no transaction in progress until transaction started")
    void haveNoTransactionInProgressUntilItStarted() {
        final TransactionalEntity entity = entityWithInactiveTx();

        assertFalse(entity.isTransactionInProgress());
    }

    @Test
    @DisplayName("have transaction in progress when transaction is active")
    void haveTransactionInProgressWhenItIsActive() {
        final TransactionalEntity entity = entityWithActiveTx(false);

        assertTrue(entity.isTransactionInProgress());
    }

    @Test
    @DisplayName("be non-changed if transaction isn't changed")
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

    @Test
    @DisplayName("fail to archive with no transaction")
    void failToArchiveWithNoTransaction() {
        assertThrows(IllegalStateException.class, () -> newEntity().setArchived(true));
    }

    @Test
    @DisplayName("fail to archive with inactive transaction")
    void failToArchiveWithInactiveTransaction() {
        final TransactionalEntity entity = entityWithInactiveTx();
        assertThrows(IllegalStateException.class, () -> entity.setArchived(true));
    }

    @Test
    @DisplayName("fail to delete with no transaction")
    void failToDeleteWithNoTransaction() {
        assertThrows(IllegalStateException.class, () -> newEntity().setDeleted(true));
    }

    @Test
    @DisplayName("fail to delete with inactive transaction")
    void failToDeleteWithInactiveTransaction() {
        final TransactionalEntity entity = entityWithInactiveTx();

        assertThrows(IllegalStateException.class, () -> entity.setDeleted(true));
    }

    @Test
    @DisplayName("return transaction `lifecycleFlags` if transaction is active")
    void returnActiveTxFlags() {
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
    @DisplayName("return non-null builder from state")
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
