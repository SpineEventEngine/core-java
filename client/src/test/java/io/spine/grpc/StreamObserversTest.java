/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.core.Response;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.spine.grpc.StreamObservers.forwardErrorsOnly;
import static io.spine.grpc.StreamObservers.fromStreamError;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.testing.Assertions.assertIllegalState;
import static io.spine.testing.TestValues.nullRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ThrowableNotThrown") // in custom assertions
@DisplayName("`StreamObservers` utility should")
class StreamObserversTest extends UtilityClassTest<StreamObservers> {

    StreamObserversTest() {
        super(StreamObservers.class);
    }

    @Test
    @DisplayName("provide non-null empty observer")
    void createEmptyObserver() {
        StreamObserver<Response> emptyObserver = noOpObserver();
        assertNotNull(emptyObserver);
        // Call methods just to add to coverage.
        emptyObserver.onNext(nullRef());
        emptyObserver.onError(nullRef());
        emptyObserver.onCompleted();
    }

    @Test
    @DisplayName("create proper error-forwarding observer")
    void createErrorForwardingObserver() {
        MemoizingObserver<?> delegate = new MemoizingObserver<>();

        var forwardingInstance = forwardErrorsOnly(delegate);

        forwardingInstance.onNext(new Object());
        forwardingInstance.onCompleted();
        var errorToForward = new RuntimeException("Sample exception");
        forwardingInstance.onError(errorToForward);

        assertThat(delegate.getError()).isEqualTo(errorToForward);
        assertThat(delegate.responses()).isEmpty();
        assertThat(delegate.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("create proper memoizing observer")
    void createMemoizingObserver() {
        var observer = memoizingObserver();

        checkFirstResponse(observer);
        checkOnNext(observer);
        checkOnError(observer);
        checkIsCompleted(observer);
    }

    private static void checkFirstResponse(MemoizingObserver<Object> observer) {
        assertIllegalState(observer::firstResponse);
    }

    private static void checkOnNext(MemoizingObserver<Object> observer) {
        assertTrue(observer.responses()
                           .isEmpty());

        var firstResponse = new Object();
        observer.onNext(firstResponse);
        assertEquals(firstResponse, observer.firstResponse());

        var sorted = ContiguousSet.create(Range.closed(1, 20),
                                          DiscreteDomain.integers());
        List<Integer> moreResponses = newArrayList(newHashSet(sorted));

        for (var element : moreResponses) {
            observer.onNext(element);
        }
        var actualResponses = observer.responses();

        assertEquals(firstResponse, actualResponses.get(0));
        assertEquals(moreResponses.size() + 1, // as there was the first response
                     actualResponses.size());
        assertEquals(moreResponses, actualResponses.subList(1, actualResponses.size()));
    }

    private static void checkOnError(MemoizingObserver<Object> observer) {
        assertNull(observer.getError());
        var exception = new RuntimeException("Sample error");
        observer.onError(exception);
        assertEquals(exception, observer.getError());
    }

    private static void checkIsCompleted(MemoizingObserver<Object> observer) {
        assertFalse(observer.isCompleted());
        observer.onCompleted();
        assertTrue(observer.isCompleted());
    }

    @Nested
    @DisplayName("return `Error` extracted")
    class ExtractError {

        @Test
        @DisplayName("from `StatusRuntimeException` metadata")
        void fromStatusRuntimeException() {
            var expectedError = Error.getDefaultInstance();
            var metadata = MetadataConverter.toMetadata(expectedError);
            var statusRuntimeException = INVALID_ARGUMENT.asRuntimeException(metadata);

            var optional = fromStreamError(statusRuntimeException);
            assertTrue(optional.isPresent());
            assertEquals(expectedError, optional.get());
        }

        @Test
        @DisplayName("from `StatusException` metadata")
        void fromStatusException() {
            var expectedError = Error.getDefaultInstance();
            var metadata = MetadataConverter.toMetadata(expectedError);
            var statusException = INVALID_ARGUMENT.asException(metadata);

            var optional = fromStreamError(statusException);
            assertTrue(optional.isPresent());
            assertEquals(expectedError, optional.get());
        }
    }

    @Nested
    @DisplayName("return absent on `Error` extraction")
    class ExtractAbsent {

        @Test
        @DisplayName("from `Throwable` which is not status exception")
        void fromGenericThrowable() {
            var msg = "Neither a StatusException nor a StatusRuntimeException.";
            var exception = new Exception(msg);

            assertFalse(fromStreamError(exception).isPresent());
        }

        @Test
        @DisplayName("from metadata without error")
        void fromMetadataWithoutError() {
            var emptyMetadata = new Metadata();
            Throwable statusRuntimeEx = INVALID_ARGUMENT.asRuntimeException(emptyMetadata);

            assertFalse(fromStreamError(statusRuntimeEx).isPresent());
        }
    }
}
