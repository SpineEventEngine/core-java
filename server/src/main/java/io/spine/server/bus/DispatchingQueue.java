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

package io.spine.server.bus;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.server.type.MessageEnvelope;

import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.bus.Buses.acknowledge;

/**
 * The queue that dispatches message envelopes in FIFO order.
 *
 * @param <E>
 *         the type of envelopes to dispatch
 */
class DispatchingQueue<E extends MessageEnvelope> {

    private final DispatchAction<E> dispatchAction;
    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();

    private boolean dispatchingInProgress;

    DispatchingQueue(DispatchAction<E> dispatchAction) {
        this.dispatchAction = dispatchAction;
    }

    /**
     * Enqueues the envelope for dispatching and dispatches it in its turn.
     *
     * <p>Once an envelope is added to the queue, the following happens: .
     * <ul>
     *     <li>if another envelope is currently being dispatched, the given envelope is enqueued
     *         as the first item and its dispatching is postponed.
     *     <li>if nothing is being dispatched and the queue is empty the envelope is dispatched
     *         right away;
     *     <li>if nothing is being dispatched and the queue is non-empty the envelope is enqueued
     *         as the first item, and the queue dispatches the last item.
     * </ul>
     *
     * <p>Once the envelope dispatching action is finished, the queue picks the next available item
     * and starts to dispatch it until the queue is empty.
     *
     * @param envelope
     *         the envelope to dispatch
     * @param observer
     *         the observer to receive the outcome of the operation
     */
    // TODO:2018-07-11:dmytro.dashenkov: Rewrite synchronization properly.
    // todo                              https://github.com/SpineEventEngine/core-java/issues/746
    public synchronized void add(E envelope, StreamObserver<Ack> observer) {
        checkNotNull(envelope);
        checkNotNull(observer);

        observer.onNext(acknowledge(envelope.id()));
        queue.add(envelope);

        if (!dispatchingInProgress()) {
            while (!queue.isEmpty()) {
                E envelopeToDispatch = queue.remove();
                dispatch(envelopeToDispatch);
            }
        }
    }

    private void dispatch(E envelope) {
        startDispatching();
        dispatchAction.dispatch(envelope);
        stopDispatching();
    }

    private void startDispatching() {
        dispatchingInProgress = true;
    }

    private void stopDispatching() {
        dispatchingInProgress = false;
    }

    private boolean dispatchingInProgress() {
        return dispatchingInProgress;
    }

    /**
     * A dispatching routine used by the queue when it's time to dispatch an envelope.
     *
     * @param <E>
     *         the type of envelopes to dispatch
     */
    public interface DispatchAction<E> {

        /**
         * Dispatches the given envelope.
         *
         * @param envelope
         *         the envelope to dispatch
         */
        void dispatch(E envelope);
    }
}
