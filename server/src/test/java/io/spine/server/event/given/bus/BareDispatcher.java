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

package io.spine.server.event.given.bus;

import com.google.common.collect.ImmutableSet;
import io.spine.server.event.EventDispatcher;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.test.event.ProjectCreated;

import java.util.Optional;
import java.util.Set;

import static io.spine.util.Exceptions.unsupported;

/**
 * A simple dispatcher class, which only dispatch and does not have own event
 * subscribing methods.
 */
public class BareDispatcher implements EventDispatcher<String> {

    private boolean dispatchCalled = false;

    @Override
    public Set<EventClass> getMessageClasses() {
        return ImmutableSet.of(EventClass.from(ProjectCreated.class));
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return ImmutableSet.of();
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        throw unsupported();
    }

    @Override
    public Set<String> dispatch(EventEnvelope event) {
        dispatchCalled = true;
        return identity();
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        // Do nothing.
    }

    public boolean isDispatchCalled() {
        return dispatchCalled;
    }
}
