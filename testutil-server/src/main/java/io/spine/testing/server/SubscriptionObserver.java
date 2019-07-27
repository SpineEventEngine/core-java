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

package io.spine.testing.server;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Internal;
import io.grpc.stub.StreamObserver;
import io.spine.client.SubscriptionUpdate;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.max;

/**
 * Counts the incoming subscription updates and feeds them to the given {@link Consumer}.
 *
 * <p>Re-throws all incoming errors as {@link IllegalStateException}.
 */
@VisibleForTesting
@Internal
public final class SubscriptionObserver implements StreamObserver<SubscriptionUpdate> {

    private final Consumer<SubscriptionUpdate> consumer;
    private final VerifyingCounter counter;

    public SubscriptionObserver(Consumer<SubscriptionUpdate> consumer) {
        this.consumer = checkNotNull(consumer);
        this.counter = new VerifyingCounter();
    }

    @Override
    public void onNext(SubscriptionUpdate update) {
        consumer.accept(update);
        updateCount(update);
    }

    @Override
    public void onError(Throwable t) {
        throw new IllegalStateException(t);
    }

    @Override
    public void onCompleted() {
        // Do nothing.
    }

    /**
     * Exposes a mutable counter that allows to verify how many updates were received at different
     * points in time.
     */
    public VerifyingCounter counter() {
        return counter;
    }

    private void updateCount(SubscriptionUpdate update) {
        int entityUpdateCount = update.getEntityUpdates()
                                      .getUpdateCount();
        int eventCount = update.getEventUpdates()
                               .getEventCount();
        int updateCount = max(entityUpdateCount, eventCount);

        counter.increment(updateCount);
    }
}
