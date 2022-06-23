/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.integration.given;

import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.integration.DocumentId;
import io.spine.server.integration.OpenOfficeDocumentUploaded;
import io.spine.server.integration.PaperDocumentScanned;
import io.spine.server.route.EventRouting;

import static io.spine.server.route.EventRoute.withId;

public class DocumentRepository extends AggregateRepository<DocumentId, DocumentAggregate> {

    @Override
    protected void setupImportRouting(EventRouting<DocumentId> routing) {
        super.setupImportRouting(routing);
        routing.route(PaperDocumentScanned.class, (message, context) -> withId(message.getId()));
    }

    @Override
    protected void setupEventRouting(EventRouting<DocumentId> routing) {
        super.setupEventRouting(routing);
        routing.route(OpenOfficeDocumentUploaded.class,
                      (message, context) -> withId(message.getId()));
    }
}
