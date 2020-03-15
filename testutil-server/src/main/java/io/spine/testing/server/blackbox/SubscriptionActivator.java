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

package io.spine.testing.server.blackbox;

import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.server.SubscriptionService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link StreamObserver} that activates the first received {@link Subscription}.
 *
 * <p>Can be specified to the {@link SubscriptionService#subscribe(Topic, StreamObserver)} method
 * to avoid any intermediate calls before subscription activation.
 *
 * <p>Re-throws all incoming errors as {@link IllegalStateException}.
 */
final class SubscriptionActivator implements StreamObserver<Subscription> {

    private final SubscriptionService subscriptionService;
    private final StreamObserver<SubscriptionUpdate> updateObserver;

    public SubscriptionActivator(SubscriptionService subscriptionService,
                                 StreamObserver<SubscriptionUpdate> updateObserver) {
        this.subscriptionService = checkNotNull(subscriptionService);
        this.updateObserver = checkNotNull(updateObserver);
    }

    @Override
    public void onNext(Subscription subscription) {
        subscriptionService.activate(subscription, updateObserver);
    }

    @Override
    public void onError(Throwable t) {
        throw new IllegalStateException(t);
    }

    @Override
    public void onCompleted() {
        // Do nothing.
    }
}
