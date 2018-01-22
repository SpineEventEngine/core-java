/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.outbus.enrich.Enricher;

/**
 * Enriches events <em>after</em> they are stored, and <em>before</em> they are dispatched.
 *
 * <p>Enrichment schema is constructed like this:
 * <pre>
 *     {@code
 *     EventEnricher enricher = EventEnricher.newBuilder()
 *         .add(ProjectId.class, String.class, new Function<ProjectId, String> { ... } )
 *         .add(ProjectId.class, UserId.class, new Function<ProjectId, UserId> { ... } )
 *         ...
 *         .build();
 *     }
 * </pre>
 *
 * @author Alexander Yevsyukov
 * @see Enricher
 */
public class EventEnricher extends Enricher<EventEnvelope, EventContext> {

    private EventEnricher(Builder builder) {
        super(builder);
    }

    /** Creates a new builder. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** The builder for {@link EventEnricher}. */
    public static class Builder extends AbstractBuilder<EventEnricher, Builder> {

        @Override
        protected EventEnricher createEnricher() {
            return new EventEnricher(this);
        }
    }
}
