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

package org.spine3.server.command;

import com.google.protobuf.Any;
import org.spine3.server.event.EventBus;

/**
 * A base interface for non-aggregate objects that handle commands.
 *
 * <p>A command handler is responsible for:
 * <ol>
 *     <li>Changing the state of the business model in response to a command.
 *     This is done by one of the command handling methods to which the handler dispatches
 *     the command.
 *     <li>Producing corresponding events.
 *     <li>Posting events to {@code EventBus}.
 * </ol>
 *
 * <p>Event messages are returned as values of command handling methods.
 *
 * <p>A command handler does not have own state. So the state of the business
 * model it changes is external to it. Even though such a behaviour may be needed in
 * some rare cases, using {@linkplain org.spine3.server.aggregate.Aggregate aggregates}
 * is a preferred way of handling commands.
 *
 * @author Alexander Yevsyukov
 */
interface ICommandHandler extends CommandDispatcher {

    /**
     * Obtains identifier of the event producer wrapped into {@link Any}.
     */
    Any getProducerId();

    /**
     * Obtains the reference to {@code EventBus} to which the handler posts the
     * produced events.
     */
    EventBus getEventBus();
}
