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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.delivery.MessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

import java.util.Optional;
import java.util.Set;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * An abstract base for {@link AbstractEventReactor}s which have state and require
 * {@linkplain io.spine.server.delivery.Delivery delivering} the messages through an
 * {@link io.spine.server.delivery.Inbox Inbox}.
 */
public abstract class AbstractStatefulReactor<I, S extends Message, B extends ValidatingBuilder<S>>
        extends AbstractEventReactor {

    private final Inbox<I> inbox;
    private final Object endpointLock = new Object();

    private B builder;

    protected B builder() {
        return builder;
    }

    protected AbstractStatefulReactor(TypeUrl stateType) {
        super();
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        this.inbox = configureInbox(delivery, stateType);
    }

    private Inbox<I> configureInbox(Delivery delivery, TypeUrl stateType) {
        Inbox.Builder<I> builder = delivery.newInbox(stateType);
        return builder.addEventEndpoint(InboxLabel.REACT_UPON_EVENT, EventEndpoint::new)
                      .build();
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(EventEnvelope event) {
        Set<I> targets = route(event);
        for (I target : targets) {
            inbox.send(event)
                 .toReactor(target);
        }
    }

    protected abstract Optional<S> load(I id);

    protected abstract void store(S updatedState);

    protected abstract B newStateBuilderWith(I id);

    protected final void flushState() {
        S newState = builder().vBuild();
        store(newState);
    }

    /**
     * Selects the target to which the event should be dispatched.
     *
     * @param event
     *         the event to dispatch
     * @return the set of the target identifiers
     */
    protected abstract ImmutableSet<I> route(EventEnvelope event);

    private final class EventEndpoint implements MessageEndpoint<I, EventEnvelope> {

        private final EventEnvelope envelope;

        private EventEndpoint(EventEnvelope envelope) {
            this.envelope = envelope;
        }

        @Override
        public void dispatchTo(I targetId) {
            synchronized (endpointLock) {
                loadIntoBuilder(targetId);
                AbstractStatefulReactor.super.dispatch(envelope);
                flushState();
            }
        }

        @Override
        public final void onDuplicate(I target, EventEnvelope envelope) {
            // do nothing.
        }

        @Override
        public Repository<I, ?> repository() {
            throw newIllegalStateException("`%s` has no repository.", getClass());
        }

        @SuppressWarnings("unchecked")
        private void loadIntoBuilder(I id) {
            Optional<S> existingState = load(id);
            if (existingState.isPresent()) {
                AbstractStatefulReactor.this.builder = (B) existingState.get()
                                                                        .toBuilder();
            } else {
                AbstractStatefulReactor.this.builder = newStateBuilderWith(id);
            }
        }
    }
}
