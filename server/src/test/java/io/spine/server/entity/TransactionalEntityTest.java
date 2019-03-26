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

import com.google.protobuf.Message;
import io.spine.core.Version;
import io.spine.server.entity.given.TeEntity;
import io.spine.server.test.shared.EmptyEntity;
import io.spine.validate.ValidatingBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.testing.TestValues.newUuidValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@DisplayName("TransactionalEntity should")
class TransactionalEntityTest {

    TransactionalEntity newEntity() {
        return new TeEntity(1L);
    }

    @Nested
    @DisplayName("be non-changed")
    class BeNonChanged {

        @Test
        @DisplayName("once created")
        void onCreation() {
            assertFalse(newEntity().isChanged());
        }

        @Test
        @DisplayName("if transaction isn't changed")
        void withUnchangedTx() {
            TransactionalEntity entity = entityWithActiveTx(false);

            assertFalse(entity.isChanged());
        }
    }

    @Nested
    @DisplayName("become changed")
    class BecomeChanged {

        @Test
        @DisplayName("if transaction state changed")
        void ifTxStateChanged() {
            TransactionalEntity entity = entityWithActiveTx(true);

            assertTrue(entity.isChanged());
        }

        @Test
        @DisplayName("once `lifecycleFlags` are updated")
        void onLifecycleFlagsUpdated() {
            TransactionalEntity entity = newEntity();
            entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                                   .setDeleted(true)
                                                   .build());
            assertTrue(entity.isChanged());
        }
    }

    @Test
    @DisplayName("have null transaction by default")
    void haveNullTxByDefault() {
        assertNull(newEntity().getTransaction());
    }

    @Nested
    @DisplayName("have no transaction in progress")
    class HaveNoTxInProgress {

        @Test
        @DisplayName("by default")
        void byDefault() {
            assertFalse(newEntity().isTransactionInProgress());
        }

        @Test
        @DisplayName("until transaction started")
        void untilTxStarted() {
            TransactionalEntity entity = entityWithInactiveTx();

            assertFalse(entity.isTransactionInProgress());
        }
    }

    @Test
    @DisplayName("have transaction in progress when transaction is active")
    void haveTxInProgress() {
        TransactionalEntity entity = entityWithActiveTx(false);

        assertTrue(entity.isTransactionInProgress());
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    @Test
    @DisplayName("allow injecting transaction")
    void allowInjectingTx() {
        TransactionalEntity entity = newEntity();
        Transaction tx = mock(Transaction.class);
        when(tx.entity()).thenReturn(entity);
        entity.injectTransaction(tx);

        assertEquals(tx, entity.getTransaction());
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    @Test
    @DisplayName("disallow injecting transaction wrapped around another entity instance")
    void disallowOtherInstanceTx() {
        TransactionalEntity entity = newEntity();
        Transaction tx = mock(Transaction.class);
        when(tx.entity()).thenReturn(newEntity());
        assertThrows(IllegalStateException.class, () -> entity.injectTransaction(tx));
    }

    @Nested
    @DisplayName("fail to archive")
    class FailToArchive {

        @Test
        @DisplayName("with no transaction")
        void withNoTx() {
            assertThrows(IllegalStateException.class, () -> newEntity().setArchived(true));
        }

        @Test
        @DisplayName("with inactive transaction")
        void withInactiveTx() {
            TransactionalEntity entity = entityWithInactiveTx();
            assertThrows(IllegalStateException.class, () -> entity.setArchived(true));
        }
    }

    @Nested
    @DisplayName("fail to delete")
    class FailToDelete {

        @Test
        @DisplayName("with no transaction")
        void withNoTx() {
            assertThrows(IllegalStateException.class, () -> newEntity().setDeleted(true));
        }

        @Test
        @DisplayName("with inactive transaction")
        void withInactiveTx() {
            TransactionalEntity entity = entityWithInactiveTx();

            assertThrows(IllegalStateException.class, () -> entity.setDeleted(true));
        }
    }

    @Test
    @DisplayName("return transaction `lifecycleFlags` if transaction is active")
    void returnActiveTxFlags() {
        TransactionalEntity entity = entityWithInactiveTx();
        LifecycleFlags originalFlags = entity.lifecycleFlags();

        LifecycleFlags modifiedFlags = originalFlags.toVBuilder()
                                                    .setDeleted(true)
                                                    .build();

        assertNotEquals(originalFlags, modifiedFlags);

        Transaction txMock = entity.getTransaction();
        assertNotNull(txMock);
        when(txMock.isActive()).thenReturn(true);
        when(txMock.lifecycleFlags()).thenReturn(modifiedFlags);

        LifecycleFlags actual = entity.lifecycleFlags();
        assertEquals(modifiedFlags, actual);
    }

    @Nested
    @DisplayName("return builder from state")
    class ReturnBuilderFromState {

        @Test
        @DisplayName("which is non-null")
        void nonNull() {
            ValidatingBuilder builder = newEntity().builderFromState();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("which reflects current state")
        void reflectingCurrentState() {
            TransactionalEntity entity = newEntity();
            Message originalState = entity.builderFromState()
                                          .build();

            EmptyEntity newState = EmptyEntity.vBuilder()
                                              .setId(newUuidValue().getValue())
                                              .build();
            assertNotEquals(originalState, newState);

            TestTransaction.injectState(entity, newState, Version.getDefaultInstance());
            Message modifiedState = entity.builderFromState()
                                          .build();

            assertEquals(newState, modifiedState);
        }
    }

    @SuppressWarnings("unchecked")  // OK for the test code.
    private TransactionalEntity entityWithInactiveTx() {
        TransactionalEntity entity = newEntity();

        Transaction tx = mock(Transaction.class);
        when(tx.isActive()).thenReturn(false);
        when(tx.entity()).thenReturn(entity);
        entity.injectTransaction(tx);
        return entity;
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private TransactionalEntity entityWithActiveTx(boolean txChanged) {
        TransactionalEntity entity = newEntity();
        Transaction tx = spy(mock(Transaction.class));
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);
        when(tx.entity()).thenReturn(entity);

        entity.injectTransaction(tx);
        return entity;
    }
}
