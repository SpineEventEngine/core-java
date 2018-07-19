/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * The {@link StreamObserver} which stores the input data and then exposes it via API calls.
 *
 * @param <T> the type of streamed objects
 * @author Alex Tymchenko
 */
@Internal
public class MemoizingObserver<T> implements StreamObserver<T> {
    
    private final List<T> responses = newLinkedList();
    private Throwable throwable;
    private boolean completed = false;

    /**
     * Creates an instance of observer, which memoizes the responses.
     *
     * @param <T> the type of objects streamed to this observer
     * @return the memoizing observer
     */
    public static <T> MemoizingObserver<T> newInstance() {
        return new MemoizingObserver<>();
    }

    /** Prevent instantiation from the outside. */
    private MemoizingObserver() {}

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
        boolean noResponses = responses.isEmpty();
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
