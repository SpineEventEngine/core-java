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
package io.spine.grpc;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import io.grpc.Metadata;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.core.Response;
import io.spine.testing.Tests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.spine.grpc.StreamObservers.forwardErrorsOnly;
import static io.spine.grpc.StreamObservers.fromStreamError;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"})
// JUnit nested classes cannot be static.
@DisplayName("StreamObservers utility should")
class StreamObserversTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(StreamObservers.class);
    }

    @Test
    @DisplayName("provide non-null empty observer")
    void createEmptyObserver() {
        StreamObserver<Response> emptyObserver = noOpObserver();
        assertNotNull(emptyObserver);
        // Call methods just to add to coverage.
        emptyObserver.onNext(Tests.nullRef());
        emptyObserver.onError(Tests.nullRef());
        emptyObserver.onCompleted();
    }

    @Test
    @DisplayName("create proper error-forwarding observer")
    void createErrorForwardingObserver() {
        @SuppressWarnings("unchecked")  // to make the mock creation look simpler.
        StreamObserver<Object> delegate = mock(StreamObserver.class);

        StreamObserver<Object> forwardingInstance = forwardErrorsOnly(delegate);

        forwardingInstance.onNext(new Object());
        forwardingInstance.onCompleted();
        RuntimeException errorToForward = new RuntimeException("Sample exception");
        forwardingInstance.onError(errorToForward);

        verify(delegate, times(1)).onError(ArgumentMatchers.eq(errorToForward));
        verify(delegate, never()).onNext(ArgumentMatchers.any());
        verify(delegate, never()).onCompleted();
    }

    @Test
    @DisplayName("create proper memoizing observer")
    void createMemoizingObserver() {
        MemoizingObserver<Object> observer = memoizingObserver();

        checkFirstResponse(observer);
        checkOnNext(observer);
        checkOnError(observer);
        checkIsCompleted(observer);
    }

    private static void checkFirstResponse(MemoizingObserver<Object> observer) {
        assertThrows(IllegalStateException.class, observer::firstResponse);
    }

    private static void checkOnNext(MemoizingObserver<Object> observer) {
        assertTrue(observer.responses()
                           .isEmpty());

        Object firstResponse = new Object();
        observer.onNext(firstResponse);
        assertEquals(firstResponse, observer.firstResponse());

        ContiguousSet<Integer> sorted = ContiguousSet.create(Range.closed(1, 20),
                                                             DiscreteDomain.integers());
        List<Integer> moreResponses = newArrayList(newHashSet(sorted));

        for (Integer element : moreResponses) {
            observer.onNext(element);
        }
        List<Object> actualResponses = observer.responses();

        assertEquals(firstResponse, actualResponses.get(0));
        assertEquals(moreResponses.size() + 1, // as there was the first response
                     actualResponses.size());
        assertEquals(moreResponses, actualResponses.subList(1, actualResponses.size()));
    }

    private static void checkOnError(MemoizingObserver<Object> observer) {
        assertNull(observer.getError());
        RuntimeException exception = new RuntimeException("Sample error");
        observer.onError(exception);
        assertEquals(exception, observer.getError());
    }

    private static void checkIsCompleted(MemoizingObserver<Object> observer) {
        assertFalse(observer.isCompleted());
        observer.onCompleted();
        assertTrue(observer.isCompleted());
    }

    @Nested
    @DisplayName("return Error extracted")
    class ExtractError {

        @Test
        @DisplayName("from StatusRuntimeException metadata")
        void fromStatusRuntimeException() {
            Error expectedError = Error.getDefaultInstance();
            Metadata metadata = MetadataConverter.toMetadata(expectedError);
            StatusRuntimeException statusRuntimeException =
                    INVALID_ARGUMENT.asRuntimeException(metadata);

            Optional<Error> optional = fromStreamError(statusRuntimeException);
            assertTrue(optional.isPresent());
            assertEquals(expectedError, optional.get());
        }

        @Test
        @DisplayName("from StatusException metadata")
        void fromStatusException() {
            Error expectedError = Error.getDefaultInstance();
            Metadata metadata = MetadataConverter.toMetadata(expectedError);
            StatusException statusException = INVALID_ARGUMENT.asException(metadata);

            Optional<Error> optional = fromStreamError(statusException);
            assertTrue(optional.isPresent());
            assertEquals(expectedError, optional.get());
        }
    }

    @Nested
    @DisplayName("return absent on Error extraction")
    class ExtractAbsent {

        @Test
        @DisplayName("from Throwable which is not status exception")
        void fromGenericThrowable() {
            String msg = "Neither a StatusException nor a StatusRuntimeException.";
            Exception exception = new Exception(msg);

            assertFalse(fromStreamError(exception).isPresent());
        }

        @Test
        @DisplayName("from metadata without error")
        void fromMetadataWithoutError() {
            Metadata emptyMetadata = new Metadata();
            Throwable statusRuntimeEx = INVALID_ARGUMENT.asRuntimeException(emptyMetadata);

            assertFalse(fromStreamError(statusRuntimeEx).isPresent());
        }
    }
}
