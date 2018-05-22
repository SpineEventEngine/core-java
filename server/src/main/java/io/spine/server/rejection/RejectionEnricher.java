/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.rejection;

import io.spine.core.RejectionContext;
import io.spine.core.RejectionEnvelope;
import io.spine.server.outbus.enrich.Enricher;

/**
 * Enriches rejections <em>after</em> they are stored, and <em>before</em> they are dispatched.
 *
 * <p>Enrichment schema is constructed like this:
 * <pre>
 *     {@code
 *     RejectionEnricher enricher = RejectionEnricher.newBuilder()
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
public class RejectionEnricher extends Enricher<RejectionEnvelope, RejectionContext> {

    private RejectionEnricher(AbstractBuilder<? extends Enricher, ?> builder) {
        super(builder);
    }

    /** Creates a new builder. */
    public static RejectionEnricher.Builder newBuilder() {
        return new RejectionEnricher.Builder();
    }

    /** The builder for {@link RejectionEnricher}. */
    public static class Builder extends AbstractBuilder<RejectionEnricher, Builder> {

        @Override
        protected RejectionEnricher createEnricher() {
            return new RejectionEnricher(this);
        }
    }
}
