/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableSet;
import io.spine.server.BoundedContext;
import io.spine.server.DefaultRepository;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.delivery.given.InboxRegistrationTestEnv.InboxProject;
import io.spine.server.delivery.given.InboxRegistrationTestEnv.InboxTask;
import io.spine.system.server.Mirror;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertWithMessage;

@DisplayName("`Inbox`es registration in `Delivery`")
@SuppressWarnings("resource" /* Ignoring the closeables for tests is OK. */)
final class InboxRegistrationTest {

    private static final String MIRROR_TYPE_URL = TypeUrl.of(Mirror.class)
                                                         .toTypeName()
                                                         .value();

    @Test
    @DisplayName("should have no overlaps for `Aggregate` mirrors from multiple Bounded Contexts")
    void registerMultipleMirrors() {
        String firstContextName = "Mirror Projects";
        buildContext(firstContextName, InboxProject.class);

        String secondContextName = "Mirror Tasks";
        buildContext(secondContextName, InboxTask.class);

        InboxDeliveries deliveries = registeredDeliveries();
        ImmutableSet<String> rawInboxTypes = deliveries.knownTypeUrls();
        assertRegistered(firstContextName, rawInboxTypes);
        assertRegistered(secondContextName, rawInboxTypes);
    }

    @SuppressWarnings("IfStatementMissingBreakInLoop")
    private static void assertRegistered(String firstContextName,
                                         ImmutableSet<String> rawInboxTypes) {
        boolean found = false;
        for (String type : rawInboxTypes) {
            if (type.contains(firstContextName) && type.contains(MIRROR_TYPE_URL)) {
                found = true;
            }
        }
        assertWithMessage("`Inbox` deliveries must have an entry " +
                                  "registered for Context `%s` and `%s` projection type.",
                          firstContextName, MIRROR_TYPE_URL)
                .that(found)
                .isTrue();
    }

    private static InboxDeliveries registeredDeliveries() {
        return ServerEnvironment.instance()
                                .delivery()
                                .registeredDeliveries();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored" /* Just triggering `Inbox` registration. */)
    private static <A extends Aggregate<String, ?, ?>>
    void buildContext(String name, Class<A> entityType) {
        BoundedContext.singleTenant(name)
                      .add(DefaultRepository.of(entityType))
                      .build();
    }
}
