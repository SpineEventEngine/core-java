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

package io.spine.core;

import io.spine.base.CommandMessage;
import io.spine.server.event.EventFactory;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.testing.core.given.GivenUserId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.event.EventFactory.on;

/**
 * Extra tests for {@link Signal}s.
 *
 * <p>The main test suite for {@code Signal}s is located in the {@code core} module. This test suite
 * only hosts tests which rely on the components from the {@code server} module.
 */
@DisplayName("`Signal` also should")
class SignalTest {

    @Test
    @DisplayName("produce correct origin")
    void produceOrigin() {
        Event originalEvent = GivenEvent.arbitrary();
        UserId producerEntityId = GivenUserId.generated();
        EventFactory factory = on(EventEnvelope.of(originalEvent), pack(producerEntityId));
        Event derivativeEvent = factory.createEvent(GivenEvent.message(), null);
        Origin origin = derivativeEvent.getContext().getPastMessage();
        assertThat(origin.getMessage())
                .isEqualTo(originalEvent.messageId());
        Origin grandOrigin = origin.getGrandOrigin();
        TypeUrl grandOriginType = TypeUrl.parse(grandOrigin.getMessage()
                                                           .getTypeUrl());
        assertThat(grandOriginType.toJavaClass())
                .isAssignableTo(CommandMessage.class);
        assertThat(grandOrigin.hasGrandOrigin())
                .isFalse();
    }
}
