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

package io.spine.testing.server.blackbox.verify.subscription;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import io.grpc.Internal;
import io.grpc.stub.StreamObserver;
import io.spine.client.SubscriptionUpdate;
import io.spine.testing.server.blackbox.verify.count.VerifyingCounter;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.Iterables.size;
import static java.util.stream.Collectors.toList;

/**
 * Transforms the received subscription updates into the Truth {@link ProtoSubject subjects} and
 * feeds to the given {@link Consumer}.
 */
@VisibleForTesting
@Internal
public final class SubscriptionObserver implements StreamObserver<SubscriptionUpdate> {

    private final Consumer<ProtoSubject<?, Message>> consumer;
    private final VerifyingCounter counter;

    public SubscriptionObserver(Consumer<ProtoSubject<?, Message>> consumer) {
        this.consumer = consumer;
        this.counter = new VerifyingCounter();
    }

    @SuppressWarnings("AvoidThrowingRawExceptionTypes") // The real type is unknown at compile time.
    @Override
    public void onNext(SubscriptionUpdate update) {
        Iterable<ProtoSubject<?, Message>> items = collectAll(update);
        items.forEach(consumer);

        int itemCount = size(items);
        counter.incrementActual(itemCount);
    }

    @Override
    public void onError(Throwable t) {
        throw new RuntimeException(t);
    }

    @Override
    public void onCompleted() {
        // Do nothing.
    }

    public VerifyingCounter counter() {
        return counter;
    }

    private static Iterable<ProtoSubject<?, Message>> collectAll(SubscriptionUpdate update) {
        switch (update.getUpdateCase()) {
            case ENTITY_UPDATES:
                return collectEntitySubjects(update);
            case EVENT_UPDATES:
                return collectEventSubjects(update);
            case UPDATE_NOT_SET:
            default:
                return ImmutableList.of();
        }
    }

    private static Iterable<ProtoSubject<?, Message>>
    collectEntitySubjects(SubscriptionUpdate update) {
        return toSubjects(update.states());
    }

    private static Iterable<ProtoSubject<?, Message>>
    collectEventSubjects(SubscriptionUpdate update) {
        return toSubjects(update.eventMessages());
    }

    private static Iterable<ProtoSubject<?, Message>>
    toSubjects(List<? extends Message> messages) {
        List<ProtoSubject<?, Message>> result =
                messages.stream()
                        .map(ProtoTruth::assertThat)
                        .collect(toList());
        return result;
    }
}
