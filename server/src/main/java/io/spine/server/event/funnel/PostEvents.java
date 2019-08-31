/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.event.funnel;

import io.spine.annotation.Internal;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The entry point into the fluent API for manually posting events into a Bounded Context.
 *
 * @see BoundedContext#postEvents()
 */
public final class PostEvents {

    private final BoundedContext context;

    /**
     * Creates a new instance.
     *
     * <p>Use the {@link BoundedContext#postEvents()} to instantiate this class instead of calling
     * the constructor directly.
     */
    @Internal
    public PostEvents(BoundedContext context) {
        this.context = checkNotNull(context);
    }

    /**
     * Specifies the actor who produces the events.
     *
     * <p>An actor may be a user of the system or a component inside the system. The {@code UserId}
     * specified as the actor should be helpful when finding out the origin of the event.
     *
     * <p>If the actor if a software component or a third-party system, rather than a human user,
     * use {@link #producedIn(String)} instead of this method.
     *
     * @param actor
     *         the ID of the user who produces the posted events
     * @return the next step in the fluent API for posting events
     */
    public AimEvents producedBy(UserId actor) {
        checkNotNull(actor);
        return new AimEvents(context, actor);
    }

    /**
     * Specifies a string identifier of the third-party system or a software component which
     * produces the events.
     *
     * <p>If the events are produced by a human user, use {@link #producedBy(UserId)} instead.
     *
     * @param thirdParty
     *         the ID of the component which produces the posted events
     * @return the next step in the fluent API for posting events
     */
    public AimEvents producedIn(String thirdParty) {
        checkNotNull(thirdParty);
        UserId actor = UserId
                .newBuilder()
                .setValue(thirdParty)
                .vBuild();
        return producedBy(actor);
    }
}
