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

package io.spine.server.outbus.enrich;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.server.outbus.enrich.given.EventMessageEnricherTestEnv.Enrichment;
import io.spine.test.event.ProjectCreated;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Litus
 */
public class MessageEnrichmentShould {

    private MessageEnrichment<ProjectCreated, ProjectCreated.Enrichment, ?> enricher;

    @Before
    public void setUp() {
        final Enricher enricher = Enrichment.newEventEnricher();
        this.enricher = MessageEnrichment.create(
                enricher,
                ProjectCreated.class,
                ProjectCreated.Enrichment.class);
    }

    @Test
    public void be_inactive_when_created() {
        assertFalse(enricher.isActive());
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Message.class, Empty.getDefaultInstance())
                .setDefault(EventContext.class, EventContext.getDefaultInstance())
                .testAllPublicInstanceMethods(enricher);
    }
}
