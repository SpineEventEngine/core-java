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

package io.spine.server.rejection;

import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link RejectionDispatcher}, that delegates the responsibilities to an aggregated
 * {@linkplain RejectionDispatcherDelegate delegate instance}.
 *
 * @param <I> the type of entity IDs to which dispatch rejections
 * @author Alexander Yevsyukov
 * @see RejectionDispatcherDelegate
 */
@Internal
public final class DelegatingRejectionDispatcher<I> implements RejectionDispatcher<I> {

    /** A target delegate. */
    private final RejectionDispatcherDelegate<I> delegate;

    private DelegatingRejectionDispatcher(RejectionDispatcherDelegate<I> delegate) {
        this.delegate = delegate;
    }

    public static <I> DelegatingRejectionDispatcher<I> of(RejectionDispatcherDelegate<I> delegate) {
        checkNotNull(delegate);
        return new DelegatingRejectionDispatcher<>(delegate);
    }

    @Override
    public Set<RejectionClass> getMessageClasses() {
        return delegate.getRejectionClasses();
    }

    @Override
    public Set<I> dispatch(RejectionEnvelope envelope) {
        checkNotNull(envelope);
        return delegate.dispatchRejection(envelope);
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        delegate.onError(envelope, exception);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("delegate", delegate.getClass())
                          .toString();
    }
}
