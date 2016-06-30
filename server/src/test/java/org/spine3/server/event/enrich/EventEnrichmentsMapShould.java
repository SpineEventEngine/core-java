/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import org.junit.Test;
import org.spine3.test.event.ProjectCreated;
import org.spine3.type.TypeName;

import java.util.Map;

import static org.junit.Assert.*;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EventEnrichmentsMapShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(EventEnrichmentsMap.class));
    }

    @Test
    public void return_map_instance() {
        final Map<TypeName, TypeName> map = EventEnrichmentsMap.getInstance();

        assertFalse(map.isEmpty());
    }

    @Test
    public void return_event_type_by_enrichment_type_name() {
        final TypeName enrichmentType = TypeName.of(ProjectCreated.Enrichment.getDescriptor());

        final TypeName eventType = EventEnrichmentsMap.getInstance()
                                                      .get(enrichmentType);

        assertEquals(TypeName.of(ProjectCreated.getDescriptor()), eventType);
    }
}
