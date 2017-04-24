/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.io;

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

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
        assertNotNull(StreamObservers.noOpObserver());
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
}
