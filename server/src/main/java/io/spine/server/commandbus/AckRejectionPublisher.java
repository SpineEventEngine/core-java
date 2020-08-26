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

package io.spine.server.commandbus;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Status;
import io.spine.server.event.EventBus;

import static io.spine.core.Status.StatusCase.REJECTION;

/**
 * An observer which publishes the rejections from the passed {@code Ack}s to an {@link EventBus}.
 */
final class AckRejectionPublisher implements StreamObserver<Ack> {

    private final EventBus eventBus;

    AckRejectionPublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onNext(Ack value) {
        Status status = value.getStatus();
        if (status.getStatusCase() == REJECTION) {
            eventBus.post(status.getRejection());
        }
    }

    @Override
    public void onError(Throwable t) {
        // NO-OP.
    }

    @Override
    public void onCompleted() {
        // NO-OP.
    }
}
