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

package io.spine.testing.server;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.option.EntityOption;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityRecordChange;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.type.TypeUrl;

import java.util.Set;

/**
 * A test implementation of {@link EntityLifecycle} which performs no action on any method call.
 *
 * @author Dmytro Dashenkov
 */
public final class NoOpLifecycle extends EntityLifecycle {

    @SuppressWarnings("TestOnlyProblems") // OK for a test utility.
    private NoOpLifecycle() {
        super(NoOpLifecycle.class.getSimpleName(),
              TypeUrl.of(Any.class),
              NoOpSystemGateway.INSTANCE);
    }

    @Override
    public void onEntityCreated(EntityOption.Kind entityKind) {
        // NoOp.
    }

    @Override
    public void onDispatchCommand(Command command) {
        // NoOp.
    }

    @Override
    public void onCommandHandled(Command command) {
        // NoOp.
    }

    @Override
    public void onCommandRejected(CommandId commandId, Event rejection) {
        // NoOp.
    }

    @Override
    public void onDispatchEventToSubscriber(Event event) {
        // NoOp.
    }

    @Override
    public void onDispatchEventToReactor(Event event) {
        // NoOp.
    }

    @Override
    protected void onStateChanged(EntityRecordChange change, Set<? extends Message> messageIds) {
        // NoOp.
    }

    @Override
    public void onTargetAssignedToCommand(CommandId id) {
        // NoOp.
    }

    public static NoOpLifecycle instance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final NoOpLifecycle value = new NoOpLifecycle();
    }
}
