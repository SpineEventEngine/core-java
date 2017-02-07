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

package org.spine3.server.procman;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.server.command.CommandBus;

/**
 * The command router that routes all commands in one call.
 *
 * @author Alexander Yevsyukov
 */
public class CommandRouter extends AbstractCommandRouter {

    CommandRouter(CommandBus commandBus, Message commandMessage, CommandContext commandContext) {
        super(commandBus, commandMessage, commandContext);
    }

    /**
     * Posts the added messages as commands to {@code CommandBus}.
     *
     * <p>The commands are posted in the order their messages were added.
     *
     * <p>The method returns after the last command was successfully posted.
     *
     * @return the event with the source and produced commands
     */
    protected CommandRouted routeAll() {
        final CommandRouted.Builder result = CommandRouted.newBuilder();
        result.setSource(getSource());

        while (hasNext()) {
            final Message message = next();
            final Command command = route(message);
            result.addProduced(command);
        }

        return result.build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected CommandRouter add(Message commandMessage) {
        super.add(commandMessage);
        return this;
    }
}
