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

import io.spine.core.Enrichment;
import io.spine.core.Enrichments;
import io.spine.core.Rejection;
import io.spine.core.RejectionContext;
import io.spine.server.rejection.given.RejectionEnrichmentConsumer;
import io.spine.test.rejection.ProjectId;
import io.spine.test.rejection.ProjectRejections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.server.rejection.given.Given.invalidProjectNameRejection;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("RejectionEnricher should")
class RejectionEnricherTest {

    /** The prefix to be used when converting a project ID to project name. */
    private static final String PROJECT_NAME_PREFIX = "PROJECT:";

    private RejectionBus rejectionBus;

    @BeforeEach
    void setUp() {
        RejectionEnricher.Builder builder = RejectionEnricher
                .newBuilder()
                .add(ProjectId.class,
                     String.class,
                     input -> PROJECT_NAME_PREFIX + input.getId());
        RejectionEnricher enricher = builder.build();

        rejectionBus = RejectionBus.newBuilder()
                                   .setEnricher(enricher)
                                   .build();
    }

    @Test
    @DisplayName("enrich rejection")
    void enrichRejection() {
        RejectionEnrichmentConsumer consumer = new RejectionEnrichmentConsumer();
        rejectionBus.register(consumer);

        Rejection rejection = invalidProjectNameRejection();
        rejectionBus.post(rejection);

        RejectionContext context = consumer.getContext();

        Enrichment enrichment = context.getEnrichment();
        assertNotEquals(Enrichment.getDefaultInstance(), enrichment);

        Optional<ProjectRejections.ProjectInfo> optional =
                Enrichments.getEnrichment(ProjectRejections.ProjectInfo.class, context);
        assertTrue(optional.isPresent());
        assertTrue(optional.get()
                           .getProjectName()
                           .startsWith(PROJECT_NAME_PREFIX));
    }
}
