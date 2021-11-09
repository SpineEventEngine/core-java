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

import io.spine.core.Event;
import io.spine.server.ContextAware;
import io.spine.server.event.EventDispatcher;
import io.spine.server.type.EventEnvelope;

/**
 * A part of some Bounded Context which defines the means of inter-Bounded Context communication.
 *
 * <p>In particular, processes the {@code external} event subscriptions, including configuration
 * and message exchange.
 *
 * @see DefaultIntegrationBroker
 */
public interface IntegrationBroker extends ContextAware, AutoCloseable {

    /**
     * Publishes the given event for other Bounded Contexts.
     *
     * <p>If this event belongs to another context, does nothing. More formally, if there is
     * a <b>subscriber</b> channel in this broker for events of such a type, those events are
     * NOT <b>published</b> from this Context.
     *
     * @param event
     *         the event to publish
     */
    void publish(EventEnvelope event);

    /**
     * Dispatches the given event via the local {@code EventBus}.
     */
    void dispatchLocally(Event event);

    /**
     * Registers a local dispatcher which is subscribed to {@code external} messages.
     *
     * @param dispatcher
     *         the dispatcher to register
     */
    void register(EventDispatcher dispatcher);

    /**
     * Unregisters a local dispatcher which should no longer be subscribed
     * to {@code external} messages.
     *
     * @param dispatcher
     *         the dispatcher to unregister
     */
    void unregister(EventDispatcher dispatcher);
}
