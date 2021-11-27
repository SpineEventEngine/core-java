/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.testing.NullPointerTester;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`CompositeObserver` should")
class CompositeObserverTest {

    @Test
    @DisplayName("not accept `null` observer list on construction")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicConstructors(CompositeObserver.class);
    }

    @Test
    @DisplayName("delegate `onNext` calls to wrapped observers")
    void delegateOnNext() {
        MemoizingObserver<String> observer1 = StreamObservers.memoizingObserver();
        MemoizingObserver<String> observer2 = StreamObservers.memoizingObserver();
        StreamObserver<String> observer =
                new CompositeObserver<>(ImmutableList.of(observer1, observer2));

        var value = "String value.";
        observer.onNext(value);

        assertThat(observer1.firstResponse()).isEqualTo(value);
        assertThat(observer2.firstResponse()).isEqualTo(value);
    }

    @Test
    @DisplayName("delegate `onError` calls to wrapped observers")
    void delegateOnError() {
        MemoizingObserver<String> observer1 = StreamObservers.memoizingObserver();
        MemoizingObserver<String> observer2 = StreamObservers.memoizingObserver();
        StreamObserver<String> observer =
                new CompositeObserver<>(ImmutableList.of(observer1, observer2));

        var error = new RuntimeException("An error.");
        observer.onError(error);

        assertThat(observer1.getError()).isEqualTo(error);
        assertThat(observer2.getError()).isEqualTo(error);
    }

    @Test
    @DisplayName("delegate `onCompleted` calls to wrapped observers")
    void delegateOnCompleted() {
        MemoizingObserver<String> observer1 = StreamObservers.memoizingObserver();
        MemoizingObserver<String> observer2 = StreamObservers.memoizingObserver();
        StreamObserver<String> observer =
                new CompositeObserver<>(ImmutableList.of(observer1, observer2));

        observer.onCompleted();

        assertThat(observer1.isCompleted()).isTrue();
        assertThat(observer2.isCompleted()).isTrue();
    }
}
