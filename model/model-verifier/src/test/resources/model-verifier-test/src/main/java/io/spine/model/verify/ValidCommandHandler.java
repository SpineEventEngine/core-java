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

package io.spine.model.verify;

import io.spine.base.EventMessage;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.event.EventBus;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * A CommandHandler with a valid command handler method.
 *
 * <p>The command handler method handles command of type {@code UInt32Value}.
 *
 * @author Dmytro Dashenkov
 */
public class ValidCommandHandler extends AbstractCommandHandler {

    protected ValidCommandHandler(EventBus eventBus) {
        super(eventBus);
    }

    @Assign
    public List<? extends EventMessage> handle(SendLink command) {
        return singletonList(LinkSent.newBuilder()
                                     .setLink(command.getLink())
                                     .build());
    }
}
