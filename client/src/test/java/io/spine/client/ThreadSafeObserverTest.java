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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.grpc.MemoizingObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.TestValues.nullRef;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`ThreadSafeObserver` should")
class ThreadSafeObserverTest {

    @Test
    @DisplayName("not accept `null`s in its ctor")
    void notAcceptNullsInCtor() {
        assertThrows(NullPointerException.class, () -> new ThreadSafeObserver<>(nullRef()));
    }

    @Test
    @DisplayName("delegate all calls to a wrapped `StreamObserver` instance")
    void delegate() {
        MemoizingObserver<Timestamp> original = new MemoizingObserver<>();
        StreamObserver<Timestamp> threadSafe = new ThreadSafeObserver<>(original);

        assertThat(original.isCompleted()).isFalse();
        threadSafe.onCompleted();
        assertThat(original.isCompleted()).isTrue();

        assertThat(original.getError()).isNull();
        Exception exception = exception();
        threadSafe.onError(exception);
        assertThat(original.getError()).isEqualTo(exception);

        assertThat(original.responses()).isEmpty();
        ImmutableList<Timestamp> values = timestamps();
        values.forEach(threadSafe::onNext);
        assertThat(original.responses()).containsExactlyElementsIn(values);
    }

    private static ImmutableList<Timestamp> timestamps() {
        ImmutableList<Timestamp> values =
                IntStream.range(0, 10)
                         .mapToObj(i -> Time.currentTime())
                         .collect(toImmutableList());
        return values;
    }

    private static NullPointerException exception() {
        NullPointerException exception = new NullPointerException("This is a test NPE.");
        return exception;
    }
}
