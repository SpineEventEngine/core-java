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

package io.spine.server.integration;

import io.spine.core.BoundedContextName;
import io.spine.protobuf.AnyPacker;
import io.spine.server.transport.ChannelId;

import java.util.function.Consumer;

/**
 * @author Alex Tymchenko
 */
final class StatusExchange extends SingleChannelExchange {

    private static final Class<BoundedContextOnline> TYPE = BoundedContextOnline.class;

    StatusExchange(TransportLink link) {
        super(link);
    }

    @Override
    ChannelId channel() {
        return Channels.statuses();
    }

    /**
     * Tells other Bounded Contexts that this context is now connected to the transport
     * and available for communication.
     */
    void declareOnlineStatus() {
        BoundedContextOnline notification =
                BoundedContextOnline.newBuilder()
                        .setContext(context())
                        .vBuild();
        ExternalMessage message = ExternalMessages.of(notification);
        publisher().publish(message.getId(), message);
    }

    /**
     * Reacts upon {@link BoundedContextOnline} message by calling the passed {@code callback}.
     */
    void onBoundedContextOnline(Consumer<BoundedContextOnline> callback) {
        subscriber().addObserver(new Observer(context(), callback));
    }

    private static final class Observer extends AbstractChannelObserver {

        private final Consumer<BoundedContextOnline> callback;

        private Observer(BoundedContextName context,
                         Consumer<BoundedContextOnline> callback) {
            super(context, TYPE);
            this.callback = callback;
        }

        @Override
        protected void handle(ExternalMessage message) {
            BoundedContextOnline msg = unpack(message);
            callback.accept(msg);
        }

        private BoundedContextOnline unpack(ExternalMessage message) {
            BoundedContextOnline msg = AnyPacker.unpack(message.getOriginalMessage(), TYPE);
            return msg;
        }
    }
}
