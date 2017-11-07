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

package io.spine.server.integration.route;

import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageChannel;
import io.spine.server.integration.Publisher;

/**
 * An abstract base for a message router. The {@code Router} is designed for routing
 * messages to {@linkplain MessageChannel message channels} based on registered
 * {@linkplain Route channel routes}.
 *
 * <p>Inspired by <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DynamicRouter.html">
 * Dynamic Router pattern.</a>
 *
 * @author Dmitry Ganzha
 */
public abstract class Router implements AutoCloseable {

    /**
     * Routes the message to corresponding {@code MessageChannel}s and returns them.
     *
     * @param message the message to be routed
     * @return returns the message channels to which the message was routed.
     */
    public abstract Iterable<Publisher> route(ExternalMessage message);

    /**
     * Registers the passed {@code Route}.
     *
     * @param route the route to register
     */
    public abstract void register(Route route);

    /**
     * Unregisters the passed {@code Route}.
     *
     * @param route the route to unregister
     */
    public abstract void unregister(Route route);

    /**
     * Returns the dead message channel. Which will be used for messages that did not find
     * a suitable message channel.
     */
    protected abstract Publisher deadMessageChannel();
}
