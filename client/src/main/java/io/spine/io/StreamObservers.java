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
package io.spine.io;

import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Response;
import io.spine.base.Responses;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.base.Responses.ok;

/**
 * A utility class for the routines related to
 * {@linkplain StreamObserver gRPC StreamObserver instances}.
 *
 * @author Alex Tymchenko
 */
@Internal
public class StreamObservers {

    private StreamObservers() {
        // Prevent from instantiation.
    }

    /**
     * Creates a {@linkplain StreamObserver observer} which does nothing upon
     * the invocation of its callback methods.
     *
     * <p>The callees which do not want to follow the responses should use this utility method
     * to eliminate boilerplate code.
     *
     * @return an instance of {@code StreamObserver} which does nothing
     */
    public static <T> StreamObserver<T> noOpObserver() {
        return new StreamObserver<T>() {
            @Override
            public void onNext(T value) {
                // Do nothing.
            }

            @Override
            public void onError(Throwable t) {
                // Do nothing.
            }

            @Override
            public void onCompleted() {
                // Do nothing.
            }
        };
    }

    /**
     * A utility method which sends {@linkplain Responses#ok() acknowledgement}
     * to the client via the {@code responseObserver} provided and
     * {@linkplain StreamObserver#onCompleted() completes} the response.
     */
    public static void ack(StreamObserver<Response> responseObserver) {
        responseObserver.onNext(ok());
        responseObserver.onCompleted();
    }

    /**
     * Wraps the given {@code delegate} into a {@code StreamObserver}, and proxy only errors to it.
     *
     * @param delegate the delegate observer
     * @param <T>      the generic parameter type of the delegating observer to be created.
     * @return delegating observer, which only proxies errors.
     */
    @Internal
    public static <T> StreamObserver<T> forwardErrorsOnly(final StreamObserver<?> delegate) {
        return new StreamObserver<T>() {
            @Override
            public void onError(Throwable t) {
                delegate.onError(t);
            }

            @Override
            public void onNext(T value) {
                // do nothing.
            }

            @Override
            public void onCompleted() {
                // do nothing.
            }
        };
    }

    /**
     * Creates an instance of observer, which memoizes the responses.
     *
     * @param <T> the type of objects streamed to this observer
     * @return the memoizing observer
     */
    @Internal
    public static <T> MemoizingObserver<T> memoizingObserver() {
        return new MemoizingObserver<>();
    }

    /**
     * The {@link StreamObserver} which stores the input data and then exposes it via API calls.
     *
     * @param <T> the type of streamed objects
     */
    @Internal
    public static class MemoizingObserver<T> implements StreamObserver<T> {
        private final List<T> responses = newLinkedList();
        private Throwable throwable;
        private boolean completed = false;

        private MemoizingObserver() {
            // prevent instantiation from the outside.
        }

        @Override
        public void onNext(T value) {
            responses.add(value);
        }

        @Override
        public void onError(Throwable t) {
            throwable = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        /**
         * Returns the first item which has been fed to this {@code StreamObserver}.
         *
         * <p>If there were no responses yet, {@link IllegalStateException} is thrown.
         *
         * @return the first item responded
         */
        public T firstResponse() {
            final boolean noResponses = responses.isEmpty();
            if (noResponses) {
                throw new IllegalStateException("No responses have been received yet");
            }
            return responses.get(0);
        }

        /**
         * Returns all the responses received so far.
         *
         * @return all objects received by this {@code StreamObserver}
         */
        public List<T> responses() {
            return newArrayList(responses);
        }

        /**
         * Allows to understand whether the response has been completed.
         *
         * @return {@code true} if the response has been completed, {@code false} otherwise
         */
        public boolean isCompleted() {
            return completed;
        }

        /**
         * Obtains the {@linkplain Throwable error} if it has been received by this observer.
         *
         * @return an error, or {@code null} if no error has been received
         */
        @Nullable
        public Throwable getError() {
            return throwable;
        }
    }

    /**
     * A {@link StreamObserver} allowing to spy on the underlying {@link StreamObserver} and
     * to perform a defined action on the calls to it's methods.
     *
     * <p>This abstract class declares no {@code abstract} methods allowing to override only those
     * methods that are really needed and ignore the rest.
     *
     * <p>By default, all the spy methods perform no action.
     *
     * @see #onNextPassed(Object) to spy for {@link #onNext(Object) onNext}
     * @see #onError(Throwable)  to spy for {@link #onError(Throwable) onError}
     * @see #onCompleted() to spy for {@link #onCompleted() onCompleted}
     */
    public abstract static class SpyObserver<T> implements StreamObserver<T> {

        private final StreamObserver<T> delegate;

        /**
         * Creates a new instance of {@code SpyObserver}.
         *
         * @param delegate the underlying {@code StreamObserver}.
         */
        protected SpyObserver(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        /**
         * Performs a custom action on {@link StreamObserver#onNext StreamObserver.onNext}.
         *
         * <p>This method is called before the actual underlying
         * {@link StreamObserver#onNext StreamObserver.onNext} invocation.
         *
         * <p>Override this method to spy on the {@code onNext} method.
         *
         * @param next the parameter of {@code StreamObserver.onNext}
         */
        @SuppressWarnings("NoopMethodInAbstractClass") // Optionally override this method
        protected void onNextPassed(T next) {
            // NoOp by default.
        }

        /**
         * Performs a custom action on
         * {@link StreamObserver#onCompleted StreamObserver.onCompleted}.
         *
         * <p>This method is called before the actual underlying
         * {@link StreamObserver#onCompleted StreamObserver.onCompleted} invocation.
         *
         * <p>Override this method to spy on the {@code onCompleted} method.
         */
        @SuppressWarnings("NoopMethodInAbstractClass") // Optionally override this method
        protected void onCompletedSteam() {
            // NoOp by default.
        }

        /**
         * Performs a custom action on {@link StreamObserver#onError StreamObserver.onError}.
         *
         * <p>This method is called before the actual underlying
         * {@link StreamObserver#onError StreamObserver.onError} invocation.
         *
         * <p>Override this method to spy on the {@code onError} method.
         *
         * @param error the parameter of {@code StreamObserver.onError}
         */
        @SuppressWarnings("NoopMethodInAbstractClass") // Optionally override this method
        protected void onErrorSpotted(Throwable error) {
            // NoOp by default.
        }

        @Override
        public final void onNext(T value) {
            onNextPassed(value);
            delegate.onNext(value);
        }

        @Override
        public final void onCompleted() {
            onCompletedSteam();
            delegate.onCompleted();
        }

        @Override
        public final void onError(Throwable error) {
            onErrorSpotted(error);
            delegate.onError(error);
        }
    }
}
