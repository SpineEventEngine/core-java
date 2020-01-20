/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.EventMessage;
import io.spine.server.delivery.event.ShardProcessingRequested;
import io.spine.server.entity.Repository;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * The framework-internal process performing the maintenance of delivery shards.
 */
class ShardMaintenanceProcess extends AbstractEventReactor {

    static final TypeUrl TYPE = TypeUrl.of(ShardMaintenance.getDefaultInstance());

    private final Inbox<ShardIndex> inbox;

    ShardMaintenanceProcess(Delivery delivery) {
        super();
        Inbox.Builder<ShardIndex> builder = delivery.newInbox(TYPE);
        builder.addEventEndpoint(InboxLabel.REACT_UPON_EVENT, EventEndpoint::new);
        this.inbox = builder.build();
    }

    //TODO:2020-01-20:alex.tymchenko: describe the need.
    @React
    Nothing on(ShardProcessingRequested event) {
        System.out.println(
                format("Received `ShardProcessingRequested` for the shard #%d.",
                       event.getId()
                            .getIndex()));
        return nothing();
    }

    @Override
    public boolean canDispatch(EventEnvelope envelope) {
        EventMessage raw = envelope.message();
        return raw instanceof ShardEvent;
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(EventEnvelope event) {
        ShardEvent message = (ShardEvent) event.message();
        inbox.send(event)
             .toReactor(message.getId());
    }

    private class EventEndpoint implements MessageEndpoint<ShardIndex, EventEnvelope> {

        private final EventEnvelope envelope;

        private EventEndpoint(EventEnvelope envelope) {
            this.envelope = envelope;
        }

        @Override
        public void dispatchTo(ShardIndex targetId) {
            ShardMaintenanceProcess.super.dispatch(envelope);
        }

        @Override
        public void onDuplicate(ShardIndex target, EventEnvelope envelope) {
            // do nothing.
        }

        @Override
        public Repository<ShardIndex, ?> repository() {
            throw newIllegalStateException("`ShardMaintenanceProcess` has no repository.");
        }
    }
}
