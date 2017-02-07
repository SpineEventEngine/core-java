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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.command.CommandBus;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The command router which produces and posts commands one by one.
 *
 * @author Alexander Yevsyukov
 */
public class IteratingCommandRouter extends AbstractCommandRouter {

    IteratingCommandRouter(CommandBus commandBus, Message commandMessage, CommandContext commandContext) {
        super(commandBus, commandMessage, commandContext);
    }

    /**
     * Routes the first of the messages and returns the message
     * to be associated with the source command.
     *
     * <p>The rest of the messages are stored and those to follow.
     *
     * @return {@code CommandRouted} message with
     * <ul>
     *     <li>the source command,
     *     <li>the first produced command,
     *     <li>the command messages for the commands that will be posted by the router later
     * </ul>
     * @see CommandRouted#getMessageToFollowList()
     */
    protected CommandRouted routeFirst() {
        final CommandRouted.Builder result = CommandRouted.newBuilder();
        result.setSource(getSource());

        final Message message = next();
        final Command command = route(message);
        result.addProduced(command);

        final Iterable<Any> iterable = new Iterable<Any>() {
            @Override
            public Iterator<Any> iterator() {
                return AnyPacker.pack(commandMessages());
            }
        };
        result.addAllMessageToFollow(iterable);

        return result.build();
    }

    /**
     * Creates and posts a next command.
     *
     * <p>The commands are created and posted in the sequence their messages were added.
     *
     * @return the posted command
     * @throws NoSuchElementException if there are no command messages to post
     * @see #hasNext()
     */
    protected Command routeNext() {
        final Message message = next();
        final Command command = route(message);
        return command;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected IteratingCommandRouter add(Message commandMessage) {
        super.add(commandMessage);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected IteratingCommandRouter addAll(Iterable<Message> iterable) {
        super.addAll(iterable);
        return this;
    }
}
