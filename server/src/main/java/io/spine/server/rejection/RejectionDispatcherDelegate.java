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

package io.spine.server.rejection;

import io.spine.annotation.Internal;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;

import java.util.Set;

/**
 * A common interface for objects which need to dispatch {@linkplain io.spine.core.Rejection
 * rejections} but are unable to implement {@link RejectionDispatcher RejectionDispatcher}.
 *
 * <p>This interface defines own contract (instead of extending
 * {@linkplain io.spine.server.bus.MessageDispatcher MessageDispatcher} to allow classes that
 * dispatch messages other than rejections (by implementing
 * {@link io.spine.server.bus.MessageDispatcher MessageDispatcher}),
 *
 * @param <I> the type of IDs of entities to which dispatch rejections
 * @author Alexander Yevsyukov
 * @see DelegatingRejectionDispatcher
 */
@Deprecated
@Internal
public interface RejectionDispatcherDelegate<I> {

    /**
     * Obtains rejection classes dispatched by this delegate.
     */
    Set<RejectionClass> getRejectionClasses();

    /**
     * Obtains external rejection classes dispatched by this delegate.
     */
    Set<RejectionClass> getExternalRejectionClasses();

    /**
     * Dispatches the rejection.
     */
    Set<I> dispatchRejection(RejectionEnvelope envelope);

    /**
     * Handles an error occurred during rejection dispatching.
     *
     * @param envelope  the rejection which caused the error
     * @param exception the error
     */
    void onError(RejectionEnvelope envelope, RuntimeException exception);
}
