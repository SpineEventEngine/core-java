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
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.EventStream;
import io.spine.validate.StringValueVBuilder;
import org.junit.Test;

import static io.spine.test.TestValues.newUuidValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public class EventPlayingEntityShould {

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(TransactionalEntityShould.class);

    @Test(expected = IllegalStateException.class)
    public void require_active_transaction_to_play_events() {
        new EpeEntity().play(
                EventStream.of(eventFactory.createEvent(StringValue.getDefaultInstance()))
        );
    }

    @Test
    public void delegate_applying_events_to_tx_when_playing() {
        final EpeEntity entity = entityWithActiveTx(false);
        final Transaction txMock = entity.getTransaction();
        assertNotNull(txMock);
        final Event firstEvent = eventFactory.createEvent(newUuidValue());
        final Event secondEvent = eventFactory.createEvent(newUuidValue());

        entity.play(EventStream.of(firstEvent, secondEvent));

        verifyEventApplied(txMock, firstEvent);
        verifyEventApplied(txMock, secondEvent);
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private static EpeEntity entityWithActiveTx(boolean txChanged) {
        final EpeEntity entity = new EpeEntity();
        final Transaction tx = spy(mock(Transaction.class));
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);
        when(tx.getEntity()).thenReturn(entity);

        entity.injectTransaction(tx);
        return entity;
    }

    private static void verifyEventApplied(Transaction txMock, Event event) {
        verify(txMock).apply(eq(EventEnvelope.of(event)));
    }

    private static class EpeEntity
            extends TransactionalEntity<Long, StringValue, StringValueVBuilder>
            implements EventPlayer {

        private EpeEntity() {
            super(0L);
        }

        @Override
        public void play(EventStream events) {
            EventPlayer.forTransactionOf(this).play(events);
        }
    }
}
