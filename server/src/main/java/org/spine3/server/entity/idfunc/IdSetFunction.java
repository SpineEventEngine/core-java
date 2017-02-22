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

package org.spine3.server.entity.idfunc;

import com.google.protobuf.Message;

import java.util.Set;

/**
 * Obtains a set of entity IDs based on an event/command message and its context.
 *
 * @param <I> the type of entity IDs
 * @param <M> the type of messages to get IDs from
 * @param <C> either {@link org.spine3.base.EventContext EventContext} or \
 *              {@link org.spine3.base.CommandContext CommandContext} type
 * @author Alexander Yevsyukov
 */
public interface IdSetFunction<I, M extends Message, C extends Message> {

    /**
     * Obtains a set of entity IDs based on the passed event or command message and its context.
     *
     * @param message an event or a command message
     * @param context either {@link org.spine3.base.EventContext EventContext} or
     *                  {@link org.spine3.base.CommandContext CommandContext} instance
     * @return a set of entity identifiers
     */
    Set<I> apply(M message, C context);
}
