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

import io.spine.core.RejectionClass;
import io.spine.server.outbus.OutputDispatcherRegistry;

import java.util.Set;

/**
 * The registry of objects dispatching the rejections to their subscribers.
 *
 * <p>There can be multiple dispatchers per rejection class.
 *
 * @author Alex Tymchenko
 */
@Deprecated
public class RejectionDispatcherRegistry
        extends OutputDispatcherRegistry<RejectionClass, RejectionDispatcher<?>> {
    /**
     * {@inheritDoc}
     *
     * Overrides to expose this method to
     * {@linkplain RejectionBus#getDispatchers(RejectionClass) RejectionBus}.
     */
    @Override
    protected Set<RejectionDispatcher<?>> getDispatchers(RejectionClass messageClass) {
        return super.getDispatchers(messageClass);
    }

    /**
     * {@inheritDoc}
     *
     * Overrides to expose this method to
     * {@linkplain RejectionBus#hasDispatchers(RejectionClass) RejectionBus}.
     */
    @Override
    protected boolean hasDispatchersFor(RejectionClass eventClass) {
        return super.hasDispatchersFor(eventClass);
    }
}
