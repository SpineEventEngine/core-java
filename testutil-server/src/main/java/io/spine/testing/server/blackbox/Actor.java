/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.testing.server.blackbox;

import io.spine.core.ActorContext;
import io.spine.core.UserId;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Time.currentTime;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;
import static io.spine.validate.Validate.checkValid;

final class Actor {

    private static final Actor defaultActor = from(BlackBoxBoundedContext.class.getName());

    private final Supplier<ActorContext> context;

    private Actor(Supplier<ActorContext> context) {
        this.context = context;
    }

    public static Actor from(String userId) {
        checkNotEmptyOrBlank(userId);
        UserId id = UserId
                .newBuilder()
                .setValue(userId)
                .build();
        return from(id);
    }

    public static Actor from(UserId userId) {
        checkNotNull(userId);
        return new Actor(() -> ActorContext
                .newBuilder()
                .setActor(userId)
                .setTimestamp(currentTime())
                .vBuild());
    }

    public static Actor with(ActorContext context) {
        checkNotNull(context);
        checkValid(context);
        return new Actor(() -> context);
    }

    public static Actor defaultActor() {
        return defaultActor;
    }

    public ActorContext context() {
        return context.get();
    }
}
