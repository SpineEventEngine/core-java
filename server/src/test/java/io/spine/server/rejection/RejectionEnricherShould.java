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

package io.spine.server.rejection;

import com.google.common.base.Function;
import com.google.protobuf.StringValue;
import io.spine.core.Enrichment;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.server.rejection.given.InvalidProjectNameReactor;
import io.spine.test.rejection.ProjectId;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;

import static io.spine.server.rejection.given.Given.invalidProjectNameRejection;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Alexander Yevsyukov
 */
public class RejectionEnricherShould {

    private RejectionBus rejectionBus;
    private RejectionEnricher enricher;

    @Before
    public void setUp() {
        final RejectionEnricher.Builder builder = RejectionEnricher.newBuilder();
        builder.add(ProjectId.class, StringValue.class,
                    new Function<ProjectId, StringValue>() {
                        @Override
                        public StringValue apply(@Nullable ProjectId input) {
                            return StringValue.newBuilder()
                                              .setValue("PROJECT:" + input.getId())
                                              .build();
                        }
                    });
        enricher = builder.build();

        rejectionBus = RejectionBus.newBuilder()
                                   .setEnricher(enricher)
                                   .build();
    }

    @Test
    public void boolean_enrich_rejection() {
        final InvalidProjectNameReactor reactor = new InvalidProjectNameReactor();
        rejectionBus.register(reactor);

        final Rejection rejection = invalidProjectNameRejection();
        rejectionBus.post(rejection);

        final RejectionEnvelope delivered = RejectionEnvelope.of(reactor.getRejectionHandled());

        assertNotEquals(Enrichment.getDefaultInstance(), delivered.getEnrichment());
    }
}
