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

import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageChannel;
import io.spine.server.integration.MessageRouted;

/**
 * The base interface for a route. The route is an entity that connects
 * {@linkplain MessageChannel message channels} and a
 * {@linkplain Router message router}.
 *
 * @author Dmitry Ganzha
 */
public interface Route {

    /**
     * Checks if the message can be routed by this route.
     *
     * <p>A route can:
     * <ul>
     * <li>accept the message (by returning {@code MessageRouted} which will indicate that
     * the message can be routed by this route);
     * <li>reject the message with description why the message is not suitable for this route;
     * </ul>
     *
     * @param message the message to be checked whether the route is suitable or not
     * @return an instance of {@code MessageRouted} which shows if the message is can be routed
     * by this route
     */
    MessageRouted accept(ExternalMessage message);

    /**
     * Obtains {@code MessageChannel}'s ID assigned to this route.
     */
    ChannelId getChannelIdentifier();
}
