/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.test.Tests;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
public class StreamObserversShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(StreamObservers.class);
    }

    @Test
    public void return_non_null_empty_observer() {
        final StreamObserver<Response> emptyObserver = noOpObserver();
        assertNotNull(emptyObserver);
        // Call methods just to add to coverage.
        emptyObserver.onNext(Tests.<Response>nullRef());
        emptyObserver.onError(Tests.<Throwable>nullRef());
        emptyObserver.onCompleted();
    }

    @Test
    public void create_proper_error_forwarding_observer() {
        @SuppressWarnings("unchecked")  // to make the mock creation look simpler.
        final StreamObserver<Object> delegate = mock(StreamObserver.class);

        final StreamObserver<Object> forwardingInstance = StreamObservers.forwardErrorsOnly(
                delegate);

        forwardingInstance.onNext(new Object());
        forwardingInstance.onCompleted();
        final RuntimeException errorToForward = new RuntimeException("Sample exception");
        forwardingInstance.onError(errorToForward);

        verify(delegate, times(1)).onError(ArgumentMatchers.eq(errorToForward));
        verify(delegate, never()).onNext(ArgumentMatchers.any());
        verify(delegate, never()).onCompleted();
    }

    @Test
    public void create_proper_memoizing_observer() {
        final MemoizingObserver<Object> observer = StreamObservers.memoizingObserver();

        checkFirstResponse(observer);
        checkOnNext(observer);
        checkOnError(observer);
        checkIsCompleted(observer);
    }

    private static void checkFirstResponse(MemoizingObserver<Object> observer) {
        try {
            observer.firstResponse();
            fail("ISE expected for the MemoizingObserver.firstResponse() if it is empty, " +
                         "but got nothing.");
        } catch (IllegalStateException e) {
            // as expected
        }
    }

    private static void checkOnNext(MemoizingObserver<Object> observer) {
        assertTrue(observer.responses().isEmpty());

        final Object firstResponse = new Object();
        observer.onNext(firstResponse);
        assertEquals(firstResponse, observer.firstResponse());

        final ContiguousSet<Integer> sorted = ContiguousSet.create(Range.closed(1, 20),
                                                                   DiscreteDomain.integers());
        final List<Integer> moreResponses = newArrayList(newHashSet(sorted));

        for (Integer element : moreResponses) {
            observer.onNext(element);
        }
        final List<Object> actualResponses = observer.responses();

        assertEquals(firstResponse, actualResponses.get(0));
        assertEquals(moreResponses.size() + 1, // as there was the first response
                     actualResponses.size());
        assertEquals(moreResponses, actualResponses.subList(1, actualResponses.size()));
    }

    private static void checkOnError(MemoizingObserver<Object> observer) {
        assertNull(observer.getError());
        final RuntimeException exception = new RuntimeException("Sample error");
        observer.onError(exception);
        assertEquals(exception, observer.getError());
    }

    private static void checkIsCompleted(MemoizingObserver<Object> observer) {
        assertFalse(observer.isCompleted());
        observer.onCompleted();
        assertTrue(observer.isCompleted());
    }

    /*
     * Error extraction tests
     **************************/

    @Test
    public void return_Error_extracted_from_StatusRuntimeException_metadata() {
        final Error expectedError = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(expectedError);
        final StatusRuntimeException statusRuntimeException =
                INVALID_ARGUMENT.asRuntimeException(metadata);

        assertEquals(expectedError, StreamObservers.fromStreamError(statusRuntimeException)
                                                   .get());
    }

    @Test
    public void return_Error_extracted_form_StatusException_metadata() {
        final Error expectedError = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(expectedError);
        final StatusException statusException = INVALID_ARGUMENT.asException(metadata);

        assertEquals(expectedError, StreamObservers.fromStreamError(statusException)
                                                   .get());
    }

    @Test
    public void return_absent_if_passed_Throwable_is_not_status_exception() {
        final String msg = "Neither a StatusException nor a StatusRuntimeException.";
        final Exception exception = new Exception(msg);

        assertFalse(StreamObservers.fromStreamError(exception)
                                   .isPresent());
    }

    @Test
    public void return_absent_if_there_is_no_error_in_metadata() {
        final Metadata emptyMetadata = new Metadata();
        final Throwable statusRuntimeEx = INVALID_ARGUMENT.asRuntimeException(emptyMetadata);

        assertFalse(StreamObservers.fromStreamError(statusRuntimeEx)
                                   .isPresent());
    }
}
