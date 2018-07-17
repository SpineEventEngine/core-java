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
package io.spine.server.delivery;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import io.spine.server.delivery.given.MessageDeliveryTestEnv.DeliveryEqualityRepository;
import io.spine.server.model.ModelTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

/**
 * @author Alex Tymchenko
 */
@DisplayName("DeliveryTag should")
class DeliveryTagTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().setDefault(Shardable.class, new DeliveryEqualityRepository())
                               .testAllPublicStaticMethods(DeliveryTag.class);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("support equality")
    void supportEquality() {
        ModelTests.clearModel();
        final DeliveryEqualityRepository repository = new DeliveryEqualityRepository();

        final DeliveryTag eventTag = DeliveryTag.forEventsOf(repository);
        final DeliveryTag secondEventTag = DeliveryTag.forEventsOf(repository);

        final DeliveryTag commandTag = DeliveryTag.forCommandsOf(repository);
        final DeliveryTag anotherCommandTag = DeliveryTag.forCommandsOf(repository);

        final DeliveryTag rejectionTag = DeliveryTag.forRejectionsOf(repository);
        final DeliveryTag anotherRejectionTag = DeliveryTag.forRejectionsOf(repository);

        new EqualsTester().addEqualityGroup(eventTag, secondEventTag)
                          .addEqualityGroup(commandTag, anotherCommandTag)
                          .addEqualityGroup(rejectionTag, anotherRejectionTag)
                          .testEquals();
    }
}
