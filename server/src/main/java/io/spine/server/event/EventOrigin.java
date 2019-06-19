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

package io.spine.server.event;

import io.spine.annotation.Internal;
import io.spine.core.ActorContext;
import io.spine.core.EventContext;
import io.spine.core.Origin;
import io.spine.server.type.MessageEnvelope;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Internal
public final class EventOrigin {

    private final @Nullable Origin otherMessage;
    private final @Nullable ActorContext importOrigin;

    private EventOrigin(@Nullable Origin otherMessage,
                        @Nullable ActorContext importOrigin) {
        this.otherMessage = otherMessage;
        this.importOrigin = importOrigin;
    }

    public static EventOrigin fromAnotherMessage(MessageEnvelope<?, ?, ?> envelope) {
        checkNotNull(envelope);
        Origin origin = envelope.asEventOrigin();
        return new EventOrigin(origin, null);
    }

    public static EventOrigin from(Origin origin) {
        checkNotNull(origin);
        return new EventOrigin(origin,null);
    }

    public static EventOrigin forImport(ActorContext actor) {
        checkNotNull(actor);
        return new EventOrigin(null, actor);
    }

    public EventContext.Builder contextBuilder() {
        EventContext.Builder builder = EventContext.newBuilder();
        if (otherMessage != null) {
            builder.setPastMessage(otherMessage);
        } else if (importOrigin != null) {
            builder.setImportContext(importOrigin);
        } else {
            throw new IllegalStateException("Event origin is undefined.");
        }
        return builder;
    }
}
