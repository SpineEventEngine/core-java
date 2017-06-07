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

package io.spine.test;

import io.grpc.stub.StreamObserver;
import io.spine.base.Response;

/**
 * Collection of observers for tests.
 *
 * @author Alexander Yevsyukov
 */
public class Observers {

    private Observers() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Returns {@code StreamObserver} that records the responses.
     *
     * <p>Use this method when you need to verify the responses of calls like
     * {@link io.spine.server.commandbus.CommandBus#post(io.spine.base.Command, StreamObserver)
     * CommandBus.post()} and similar methods.
     *
     * <p>Returns a fresh instance upon every call to avoid state clashes.
     */
    public static MemoizingObserver memoizingObserver() {
        return new MemoizingObserver();
    }

    /**
     * The {@code StreamObserver} recording the responses.
     *
     * @see #memoizingObserver()
     */
    public static class MemoizingObserver implements StreamObserver<Response> {

        private Response response;
        private Throwable throwable;
        private boolean completed = false;

        protected MemoizingObserver() {
        }

        @Override
        public void onNext(Response response) {
            this.response = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        public Response getResponse() {
            return response;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isCompleted() {
            return this.completed;
        }
    }
}
