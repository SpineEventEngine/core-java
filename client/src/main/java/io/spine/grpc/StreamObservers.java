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

import io.grpc.Metadata;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.core.Response;
import io.spine.core.Responses;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Responses.ok;

/**
 * A utility class for the routines related to
 * {@linkplain StreamObserver gRPC StreamObserver instances}.
 */
public class StreamObservers {

    /** Prevents instantiation of this utility class. */
    private StreamObservers() {
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
        return new NoOpObserver<>();
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
    public static <T> StreamObserver<T> forwardErrorsOnly(StreamObserver<?> delegate) {
        return new ErrorForwardingObserver<>(delegate);
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
     * Extracts a {@linkplain Error system error} from the
     * {@linkplain StreamObserver#onError(Throwable) Throwable},
     * received on a client-side as a result of a failed gRPC call to server-side routines.
     *
     * <p>The {@code Error} is extracted from the trailer metadata of
     * either {@link StatusRuntimeException} or {@link StatusException} only.
     *
     * <p>If any other type of {@code Throwable} is passed, {@code Optional.empty()} is returned.
     *
     * @param throwable the {@code Throwable} to extract an {@link Error}
     * @return the extracted error or {@code Optional.empty()} if the extraction failed
     */
    @SuppressWarnings("ChainOfInstanceofChecks") // Only way to check an exact throwable type.
    public static Optional<Error> fromStreamError(Throwable throwable) {
        checkNotNull(throwable);

        if (throwable instanceof StatusRuntimeException) {
            Metadata metadata = ((StatusRuntimeException) throwable).getTrailers();
            return MetadataConverter.toError(metadata);
        }
        if (throwable instanceof StatusException) {
            Metadata metadata = ((StatusException) throwable).getTrailers();
            return MetadataConverter.toError(metadata);
        }

        return Optional.empty();
    }

    /**
     * An observer which does nothing.
     *
     * @param <T> the type of the observable value
     */
    private static class NoOpObserver<T> implements StreamObserver<T> {

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

        @Override
        public String toString() {
            return "StreamObservers.noOpObserver()";
        }
    }

    /**
     * An observer which forward error handling to the passed delegate.
     * Otherwise does nothing.
     *
     * @param <T> the type of the observable value
     */
    private static class ErrorForwardingObserver<T> implements StreamObserver<T> {

        private final StreamObserver<?> delegate;

        private ErrorForwardingObserver(StreamObserver<?> delegate) {
            this.delegate = delegate;
        }

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

        @Override
        public String toString() {
            return "StreamObservers.forwardErrorsOnly()";
        }
    }
}
