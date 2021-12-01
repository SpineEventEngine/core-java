/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.delivery;

import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newConcurrentMap;

/**
 * The container for the {@linkplain ShardedMessageDelivery message deliveries} for each
 * {@link Inbox} registered.
 */
final class InboxDeliveries {

    private final Map<String, ShardedMessageDelivery<InboxMessage>> contents = newConcurrentMap();

    /**
     * Obtains the proper delivery for the given type URL of a message target.
     *
     * @throws IllegalStateException
     *         if there is no delivery found
     */
    ShardedMessageDelivery<InboxMessage> get(String typeUrl) {
        var result = contents.get(typeUrl);
        checkState(result != null,
                   "Cannot find the registered Inbox for the type URL `%s`.", typeUrl);
        return result;
    }

    /**
     * Obtains the proper delivery for the message passed according to the type URL of its target.
     *
     * @throws IllegalStateException
     *         if there is no delivery found
     */
    ShardedMessageDelivery<InboxMessage> get(InboxMessage message) {
        var typeUrl = message.getInboxId()
                             .getTypeUrl();
        return get(typeUrl);
    }

    /**
     * Registers the given {@code Inbox}.
     */
    void register(Inbox<?> inbox) {
        var entityType = inbox.entityStateType();
        contents.put(entityType.value(), inbox.delivery());
    }

    /**
     * Unregisters the given {@code Inbox}.
     *
     * <p>If there was no such {@code Inbox} registered, does nothing.
     */
    void unregister(Inbox<?> inbox) {
        var entityType = inbox.entityStateType();
        contents.remove(entityType.value());
    }
}
