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

package org.spine3.server.event.enrich;

import org.junit.Before;
import org.junit.Test;
import org.spine3.server.event.Given;
import org.spine3.test.event.ProjectCreated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Litus
 */
public class EventMessageEnricherShould {

    private EventMessageEnricher<ProjectCreated, ProjectCreated.Enrichment> enricher;

    @Before
    public void setUp() {
        final EventEnricher eventEnricher = Given.Enrichment.newEventEnricher();
        this.enricher = EventMessageEnricher.newInstance(
                eventEnricher,
                ProjectCreated.class,
                ProjectCreated.Enrichment.class);
    }

    @Test
    public void be_inactive_when_created() {
        assertFalse(enricher.isActive());
    }

    @Test(expected = NullPointerException.class)
    public void throw_NPE_if_pass_null() {
        enricher.activate();
        enricher.apply(null);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_if_activate_not_called_before_apply() {
        enricher.apply(ProjectCreated.getDefaultInstance());
    }

    @Test
    public void return_itself_on_get_function() {
        assertEquals(enricher, enricher.getFunction());
    }
}
