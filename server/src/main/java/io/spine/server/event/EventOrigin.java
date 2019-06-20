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

/**
 * Origin of an event.
 *
 * <p>An event may originate from:
 * <ol>
 *     <li>another signal, command or event;
 *     <li>actor in the case of an import.
 * </ol>
 */
@Internal
public final class EventOrigin {

    private final @Nullable Origin otherMessage;
    private final @Nullable ActorContext importOrigin;

    private EventOrigin(@Nullable Origin otherMessage,
                        @Nullable ActorContext importOrigin) {
        this.otherMessage = otherMessage;
        this.importOrigin = importOrigin;
    }

    /**
     * Creates an {@code EventOrigin} from the given message.
     *
     * @param envelope
     *         the parent message
     */
    public static EventOrigin fromAnotherMessage(MessageEnvelope<?, ?, ?> envelope) {
        checkNotNull(envelope);
        Origin origin = envelope.asEventOrigin();
        return new EventOrigin(origin, null);
    }

    /**
     * Creates an {@code EventOrigin} from a pre-assembled {@link Origin}.
     */
    public static EventOrigin from(Origin origin) {
        checkNotNull(origin);
        return new EventOrigin(origin,null);
    }

    /**
     * Creates an {@code EventOrigin} for the case of an event import.
     *
     * @param actor
     *         the context the actor importing the event
     */
    public static EventOrigin forImport(ActorContext actor) {
        checkNotNull(actor);
        return new EventOrigin(null, actor);
    }

    /**
     * Creates a new {@link EventContext.Builder} with this origin.
     *
     * <p>For a parent message origin, the {@code past_message} attribute is populated.
     *
     * <p>For an import origin, the {@code import_context} attribute is populated.
     *
     * @return new {@code EventContext} builder
     */
    EventContext.Builder contextBuilder() {
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
