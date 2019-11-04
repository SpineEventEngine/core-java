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

package io.spine.client;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintains the list of subscriptions created by the {@link Client} which are
 * not cancelled yet.
 *
 * <p>The {@link Client} updates this collection when a new subscription is
 * {@linkplain Client#subscribeTo(Topic, StreamObserver) created} and
 * {@linkplain Client#cancel(Subscription) canceled}.
 *
 * <p>Remaining subscriptions are cancelled by the {@code Client} upon
 * {@linkplain #cancelAll(Client) request}.
 */
final class ActiveSubscriptions {

    private final List<Subscription> subscriptions = new ArrayList<>();

    /** Remembers the passed subscription for future use. */
    void remember(Subscription s) {
        subscriptions.add(checkNotNull(s));
    }

    /** Forgets the passed subscription. */
    void forget(Subscription s) {
        subscriptions.remove(checkNotNull(s));
    }

    /** Cancels all the remembered subscription. */
    void cancelAll(Client client) {
        // Use the loop approach to avoid concurrent modification because the `Client` modifies
        // active subscriptions when canceling.
        while (!subscriptions.isEmpty()) {
            Subscription subscription = subscriptions.get(0);
            client.cancel(subscription);
        }
    }

    @VisibleForTesting
    boolean contains(Subscription s) {
        return subscriptions.contains(s);
    }

    @VisibleForTesting
    boolean isEmpty() {
        return subscriptions.isEmpty();
    }
}
