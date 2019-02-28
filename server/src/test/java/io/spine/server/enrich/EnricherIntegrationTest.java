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

package io.spine.server.enrich;

import io.spine.server.enrich.given.EitEnricherSetup;
import io.spine.server.enrich.given.EitProjectRepository;
import io.spine.server.enrich.given.EitTaskRepository;
import io.spine.server.enrich.given.EitUserRepository;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Enricher should")
@Disabled("Until new enrichment implementation is finalized")
public class EnricherIntegrationTest {

    private BlackBoxBoundedContext context;

    @BeforeEach
    void setUp() {
        EitProjectRepository projects = new EitProjectRepository();
        EitTaskRepository tasks = new EitTaskRepository();
        EitUserRepository users = new EitUserRepository();
        context = BlackBoxBoundedContext
                .multiTenant(EitEnricherSetup.createEnricher(users, projects, tasks))
                .with(projects)
                .with(tasks)
                .with(users);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("have builder")
    void haveBuilder() {
        assertNotNull(Enricher.newBuilder());
    }
}
