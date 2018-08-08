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

package io.spine.system.server;

import io.spine.core.CommandId;
import io.spine.core.Subscribe;
import io.spine.server.entity.IdempotencySpec;
import io.spine.server.projection.Projection;

/**
 * @author Dmytro Dashenkov
 */
public class HandledCommandsProjection
        extends Projection<EntityHistoryId, HandledCommands, HandledCommandsVBuilder> {

    public HandledCommandsProjection(EntityHistoryId id) {
        super(id);
    }

    @Subscribe
    public void on(EntityCreated event) {
        IdempotencySpec idempotency = event.getRepositorySpec()
                                           .getIdempotency();
        getBuilder().setId(event.getId())
                    .setMemoryLimit(idempotency.getCommandIdempotencyThreashold());
    }

    @Subscribe
    public void on(CommandDispatchedToHandler event) {
        CommandId commandId = event.getPayload()
                                   .getCommand();
        getBuilder().addCommand(commandId);
        trimTail();
    }

    private void trimTail() {
        HandledCommandsVBuilder builder = getBuilder();
        int size = builder.getCommand().size();
        int limit = builder.getMemoryLimit();
        while (size > limit) {
            int lastElementIndex = size - 1;
            builder.removeCommand(lastElementIndex);

            size = builder.getCommand().size();
        }
    }
}
