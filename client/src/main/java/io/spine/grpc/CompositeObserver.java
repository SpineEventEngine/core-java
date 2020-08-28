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

package io.spine.grpc;

import com.google.common.collect.ImmutableList;
import io.grpc.stub.StreamObserver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A stream observer which delegates calls to multiple other observers with the same target type.
 *
 * @param <T>
 *         the observed type
 */
public final class CompositeObserver<T> implements StreamObserver<T> {

    private final ImmutableList<StreamObserver<? super T>> observers;

    public CompositeObserver(Iterable<StreamObserver<? super T>> observers) {
        checkNotNull(observers);
        this.observers = ImmutableList.copyOf(observers);
    }

    @Override
    public void onNext(T value) {
        observers.forEach(o -> o.onNext(value));
    }

    @Override
    public void onError(Throwable t) {
        observers.forEach(o -> o.onError(t));
    }

    @Override
    public void onCompleted() {
        observers.forEach(StreamObserver::onCompleted);
    }
}
