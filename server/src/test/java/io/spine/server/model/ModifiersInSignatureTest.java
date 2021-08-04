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

import io.spine.model.contexts.projects.event.SigProjectCreated;
import io.spine.server.event.model.SubscriberSignature;
import io.spine.server.model.handler.given.InvalidProtectedSubscriber;
import io.spine.server.model.handler.given.ValidProtectedSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.server.model.AccessModifier.PROTECTED_WITH_OVERRIDE;

@DisplayName("`MethodSignature` access modifiers should")
class ModifiersInSignatureTest {

    @Test
    @DisplayName("allow for protected methods which are overridden from a superclass")
    void validProtected() throws NoSuchMethodException {
        Method method = ValidProtectedSubscriber.class
                .getDeclaredMethod("overridingProtected", SigProjectCreated.class);
        AccessModifier foundModifier = new SubscriberSignature()
                .modifier()
                .stream()
                .filter(m -> m.test(method))
                .findFirst()
                .orElseGet(Assertions::fail);
        assertThat(foundModifier)
                .isSameInstanceAs(PROTECTED_WITH_OVERRIDE);
    }

    @Test
    @DisplayName("not allow for protected methods in general")
    void invalidProtected() throws NoSuchMethodException {
        Method method = InvalidProtectedSubscriber.class
                .getDeclaredMethod("justForTheLols", SigProjectCreated.class);
        Optional<AccessModifier> foundModifier = new SubscriberSignature()
                .modifier()
                .stream()
                .filter(m -> m.test(method))
                .findFirst();
        assertThat(foundModifier)
                .isEmpty();
    }
}
