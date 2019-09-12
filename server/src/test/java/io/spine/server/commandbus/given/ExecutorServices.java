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

package io.spine.server.commandbus.given;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Provides the different variations of {@link java.util.concurrent.ExecutorService}.
 */
public final class ExecutorServices {

    /** Prevents instantiation of this test env class. */
    private ExecutorServices() {
    }

    /**
     * A NO-OP executor service.
     *
     * <p>In tests, it's often necessary to check that the operation is scheduled, but its actual
     * execution is redundant and can even lead to errors.
     *
     * <p>For such cases, the {@code NoOpScheduledThreadPoolExecutor} provides a NO-OP "schedule"
     * operation.
     */
    public static final class NoOpScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

        /**
         * Constructs a new instance.
         *
         * <p>Specifies {@code 1} as a thread count because it doesn't play any part for this
         * executor.
         */
        public NoOpScheduledThreadPoolExecutor() {
            super(1);
        }

        /**
         * Does nothing.
         *
         * <p>Returns {@code null} as the result of {@code schedule} for command bus is always
         * ignored.
         */
        @Override
        public @Nullable ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return null;
        }

        /**
         * Does nothing.
         *
         * <p>Returns {@code null} as the result of {@code schedule} for command bus is always
         * ignored.
         */
        @Override
        public <V> @Nullable ScheduledFuture<V>
        schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return null;
        }
    }

    /**
     * An executor service which always schedules an operation to throw exception.
     *
     * <p>Can be used to emulate the failure in the scheduled operation.
     */
    public static final class ThrowingThreadPoolExecutor extends ScheduledThreadPoolExecutor {

        private static final String ERROR_MESSAGE = "Ignore this exception.";

        private final ScheduledExecutorService delegate;

        private boolean throwScheduled;

        /**
         * Constructs a new instance.
         *
         * <p>Specifies {@code 1} as a thread count because it's almost always enough for operation
         * failure tests.
         */
        public ThrowingThreadPoolExecutor(ScheduledExecutorService delegate) {
            super(1);
            this.delegate = delegate;
        }

        /**
         * Always schedules an operation to throw an {@link IllegalStateException}.
         */
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            throwScheduled = true;
            return delegate.schedule(() -> {
                throw newIllegalStateException(ERROR_MESSAGE);
            }, delay, unit);
        }

        /**
         * Always schedules an operation to throw an {@link IllegalStateException}.
         */
        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throwScheduled = true;
            return delegate.schedule(() -> {
                throw newIllegalStateException(ERROR_MESSAGE);
            }, delay, unit);
        }

        public boolean throwScheduled() {
            return throwScheduled;
        }
    }
}
