/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.collect.ImmutableSet;
import io.spine.server.model.given.external.TestCommander;
import io.spine.server.model.given.external.TestReactor;
import io.spine.server.model.given.external.TestSubscriber;
import io.spine.server.type.EventClass;
import io.spine.test.model.external.ExtProjectCreated;
import io.spine.test.model.external.ExtProjectStarted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

@DisplayName("External method handler can be declared")
class ExternalAttributeTest {

    @Nested
    @DisplayName("with `@Subscribe`")
    class Subscribe extends Suite {

        Subscribe() {
            super(new TestSubscriber().externalEventClasses());
        }
    }

    @Nested
    @DisplayName("with `@React`")
    class React extends Suite {

        React() {
            super(new TestReactor().externalEventClasses());
        }
    }

    @Nested
    @DisplayName("with `@Command`")
    class Command extends Suite {

        Command() {
            super(new TestCommander().externalEvents());
        }
    }

    @SuppressWarnings("AbstractClassWithoutAbstractMethods") // Makes JUnit ignore this class.
    abstract static class Suite {

        private final ImmutableSet<EventClass> externalEventClasses;

        Suite(ImmutableSet<EventClass> externalEventClasses) {
            this.externalEventClasses = checkNotNull(externalEventClasses);
        }

        @Test
        @DisplayName("via param annotation")
        void annotation() {
            assertThat(externalEventClasses)
                    .contains(EventClass.from(ExtProjectCreated.class));
        }

        @Test
        @DisplayName("via annotation attribute")
        void attribute() {
            assertThat(externalEventClasses)
                    .contains(EventClass.from(ExtProjectStarted.class));
        }
    }
}
