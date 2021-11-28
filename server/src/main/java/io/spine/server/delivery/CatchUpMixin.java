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

import io.spine.annotation.GeneratedMixin;
import io.spine.annotation.Internal;

/**
 * A mixin for the state of the {@linkplain CatchUpProcess catch-up process job}.
 */
@GeneratedMixin
@Internal
interface CatchUpMixin extends CatchUpOrBuilder {

    /**
     * Tells whether the passed {@code InboxMessage} matches this catch-up job.
     *
     * <p>To match, two conditions must be met:
     *
     * <ol>
     *     <li>the target entity type of the job and the message must be the same;
     *
     *     <li>the identifier of the message target must be included into the list of the
     *     identifiers specified in the job OR the job matches all the targets of the entity type.
     * </ol>
     *
     * @param message
     *         the message to match to the job
     * @return {@code true} if the message matches the job, {@code false} otherwise
     */
    default boolean matches(InboxMessage message) {
        var expectedProjectionType = getId().getProjectionType();
        var targetInbox = message.getInboxId();
        var actualTargetType = targetInbox.getTypeUrl();
        if (!expectedProjectionType.equals(actualTargetType)) {
            return false;
        }
        var targets = getRequest().getTargetList();
        if (targets.isEmpty()) {
            return true;
        }
        var rawEntityId = targetInbox.getEntityId().getId();
        return targets.stream()
                      .anyMatch((t) -> t.equals(rawEntityId));
    }
}
