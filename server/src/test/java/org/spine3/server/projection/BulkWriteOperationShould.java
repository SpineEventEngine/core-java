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
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Durations2;
import org.spine3.server.projection.BulkWriteOperation.FlushCallback;
import org.spine3.test.TimeTests;
import org.spine3.test.projection.Project;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
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
        final Duration duration = Durations2.seconds(60);
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

    @SuppressWarnings("MagicNumber")
    @Test
    public void complete_on_timeout() throws InterruptedException {
        final Duration duration = Durations2.nanos(1L);
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

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void store_given_projections_in_memory() {
        final int projectionsCount = 5;
        final Set<TestProjection> projections = new HashSet<>(projectionsCount);
        for (int i = 0; i < projectionsCount; i++) {
            projections.add(new TestProjection(i));
        }
        final Timestamp whenHandled = Timestamp.getDefaultInstance();
        final BulkWriteOperation<Object, TestProjection> operation = newOperation(projections, whenHandled);
        assertTrue(operation.isInProgress());
        for (TestProjection projection : projections) {
            operation.storeProjection(projection);
        }
        operation.complete();
        assertFalse(operation.isInProgress());
    }

    @Test
    public void store_given_timestamp_in_memory() {
        final Set<TestProjection> projections = emptySet();
        final Timestamp whenHandled = TimeTests.Past.secondsAgo(5L);
        final BulkWriteOperation<Object, TestProjection> operation = newOperation(projections, whenHandled);
        assertTrue(operation.isInProgress());

        operation.storeLastHandledEventTime(whenHandled);

        operation.complete();
        assertFalse(operation.isInProgress());
    }

    @Test
    public void store_the_very_last_timestamp_only() {
        final Set<TestProjection> projections = emptySet();
        final Timestamp firstEvent = TimeTests.Past.secondsAgo(5L);
        final Timestamp secondEvent = TimeTests.Past.secondsAgo(5L);
        final Timestamp lastEvent = TimeTests.Past.secondsAgo(5L);

        final BulkWriteOperation<Object, TestProjection> operation = newOperation(projections, lastEvent);
        assertTrue(operation.isInProgress());

        operation.storeLastHandledEventTime(firstEvent);
        operation.storeLastHandledEventTime(secondEvent);
        operation.storeLastHandledEventTime(lastEvent);

        operation.complete();
        assertFalse(operation.isInProgress());
    }

    private static BulkWriteOperation<Object, TestProjection> newOperation() {
        final Duration duration = Durations2.seconds(100);
        final BulkWriteOperation<Object, TestProjection> operation
                = new BulkWriteOperation<>(duration, new EmptyCallback());
        return operation;
    }

    private static BulkWriteOperation<Object, TestProjection> newOperation(
            Set<TestProjection> projections,
            Timestamp lastHandldEventTime) {
        final Duration duration = Durations2.seconds(100);
        final BulkWriteOperation<Object, TestProjection> operation
                = new BulkWriteOperation<>(duration, new AssertResults(projections, lastHandldEventTime));
        return operation;
    }

    private static class EmptyCallback implements FlushCallback<TestProjection> {

        @Override
        public void onFlushResults(Set<TestProjection> projections, Timestamp lastHandledEventTime) {
            assertNotNull(projections);
            assertNotNull(lastHandledEventTime);
        }
    }

    private static class AssertResults implements FlushCallback<TestProjection> {

        private final Set<TestProjection> projections;
        private final Timestamp lastEventTime;

        private AssertResults(Set<TestProjection> projections, Timestamp lastEventTime) {
            this.projections = projections;
            this.lastEventTime = lastEventTime;
        }

        @Override
        public void onFlushResults(Set<TestProjection> projections, Timestamp lastHandledEventTime) {
            assertEquals(this.projections.size(), projections.size());
            assertTrue(projections.containsAll(this.projections));

            assertEquals(this.lastEventTime, lastHandledEventTime);
        }
    }

    private static class TestProjection extends Projection<Object, Project> {

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        protected TestProjection(Object id) {
            super(id);
        }
    }
}
