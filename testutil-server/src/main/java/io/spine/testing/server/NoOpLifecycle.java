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

package io.spine.testing.server;

import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EventFilter;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.type.TypeUrl;

/**
 * A test implementation of {@link EntityLifecycle} which performs no action on any method call.
 */
public final class NoOpLifecycle extends EntityLifecycle {

    private static final NoOpLifecycle INSTANCE = new NoOpLifecycle();

    @SuppressWarnings("TestOnlyProblems") // OK for a test utility.
    private NoOpLifecycle() {
        super(NoOpLifecycle.class.getSimpleName(),
              TypeUrl.of(Any.class),
              NoOpSystemWriteSide.INSTANCE,
              EventFilter.allowAll());
    }

    public static NoOpLifecycle instance() {
        return INSTANCE;
    }

    @Override
    protected void postCommand(CommandMessage command) {
        // NoOp.
    }

    @Override
    protected void postEvent(EventMessage event) {
        // NoOp.
    }

    @Override
    protected void postNotification(EventMessage event) {
        // NoOp.
    }
}
