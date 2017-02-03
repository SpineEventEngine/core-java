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

package org.spine3.server.projection;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Durations;
import org.spine3.server.projection.BulkWriteOperation.FlushCallback;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Dmytro Dashenkov
 */
public class BulkWriteOperationShould {

    @Test
    public void initialize_with_proper_delay_and_callback() {
        final Duration duration = Durations.seconds(60);
        final BulkWriteOperation operation = new BulkWriteOperation<>(duration, new EmptyCallback());
        assertNotNull(operation);
    }

    @Test
    public void return_own_in_progress_state() {
        final BulkWriteOperation operation = newOperation();
        assertTrue(operation.isInProgress());
        operation.complete();
        assertFalse(operation.isInProgress());
    }

    @Test
    public void do_nothing_if_checked_before_completion_time() {
        final BulkWriteOperation operation = newOperation();
        assertTrue(operation.isInProgress());
        operation.checkExpiration();
        assertTrue(operation.isInProgress());
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_complete_twice() {
        final BulkWriteOperation operation = newOperation();
        assertTrue(operation.isInProgress());

        operation.complete();
        assertFalse(operation.isInProgress());
        operation.complete();
    }

    @Test
    public void close_on_complete() {
        final BulkWriteOperation operationSpy = spy(newOperation());

        operationSpy.complete();
        verify(operationSpy).close();
    }

    @Test
    public void exit_silently_if_cheked_after_completion() {
        final BulkWriteOperation operation = newOperation();
        operation.complete();
        assertFalse(operation.isInProgress());
        operation.checkExpiration();
        assertFalse(operation.isInProgress());
    }

    @Test
    public void complete_on_timeout() throws InterruptedException {
        final Duration duration = Durations.nanos(1L);
        final FlushCallback callback = spy(new EmptyCallback());
        @SuppressWarnings("unchecked") // Due to `spy` usage
        final BulkWriteOperation operation = spy(new BulkWriteOperation(duration, callback));
        assertTrue(operation.isInProgress());

        Thread.sleep(10L);

        operation.checkExpiration();
        assertFalse(operation.isInProgress());
        verify(operation).complete();
        verify(callback).onFlushResults(any(Set.class), any(Timestamp.class));
    }

    private static BulkWriteOperation newOperation() {
        final Duration duration = Durations.seconds(42);
        final BulkWriteOperation operation = new BulkWriteOperation<>(duration, new EmptyCallback());
        return operation;
    }

    private static class EmptyCallback implements FlushCallback<Projection<Object, Message>> {

        @Override
        public void onFlushResults(Set<Projection<Object, Message>> projections, Timestamp lastHandledEventTime) {
            assertNotNull(projections);
            assertNotNull(lastHandledEventTime);
        }
    }
}
