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

import com.google.protobuf.StringValue;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.testing.server.TestEventFactory;
import io.spine.validate.StringValueVBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.testing.TestValues.newUuidValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("TransactionalEventPlayer should")
class TransactionalEventPlayerTest {

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(TransactionalEntityTest.class);

    @Test
    @DisplayName("require active transaction to play events")
    void requireActiveTx() {
        assertThrows(IllegalStateException.class,
                     () -> new TxPlayingEntity().play(
                             eventFactory.createEvent(StringValue.getDefaultInstance())));
    }

    @Test
    @DisplayName("delegate applying events to transaction when playing")
    void delegateEventsToTx() {
        TxPlayingEntity entity = entityWithActiveTx(false);
        Transaction txMock = entity.getTransaction();
        assertNotNull(txMock);
        Event firstEvent = eventFactory.createEvent(newUuidValue());
        Event secondEvent = eventFactory.createEvent(newUuidValue());

        entity.play(newArrayList(firstEvent, secondEvent));

        verifyEventApplied(txMock, firstEvent);
        verifyEventApplied(txMock, secondEvent);
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private static TxPlayingEntity entityWithActiveTx(boolean txChanged) {
        TxPlayingEntity entity = new TxPlayingEntity();
        Transaction tx = spy(mock(Transaction.class));
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);
        when(tx.getEntity()).thenReturn(entity);

        entity.injectTransaction(tx);
        return entity;
    }

    private static void verifyEventApplied(Transaction txMock, Event event) {
        verify(txMock).apply(eq(EventEnvelope.of(event)));
    }

    private static class TxPlayingEntity
            extends TransactionalEntity<Long, StringValue, StringValueVBuilder>
            implements EventPlayer {

        private TxPlayingEntity() {
            super(0L);
        }

        @Override
        public void play(Iterable<Event> events) {
            EventPlayers.forTransactionOf(this).play(events);
        }
    }
}
