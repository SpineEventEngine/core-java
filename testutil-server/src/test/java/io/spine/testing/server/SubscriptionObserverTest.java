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

package io.spine.testing.server;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.Response;
import io.spine.core.Status;
import io.spine.testing.server.given.GivenSubscriptionUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SubscriptionObserver should")
class SubscriptionObserverTest {

    @Test
    @DisplayName("reject `null` consumer on construction")
    void rejectNullConsumer() {
        new NullPointerTester()
                .testAllPublicConstructors(SubscriptionObserver.class);
    }

    @Test
    @DisplayName("feed the received update to the consumer")
    void feedUpdateToConsumer() {
        Consumer<SubscriptionUpdate> consumer = update -> ProtoTruth.assertThat(update)
                                                                    .isEqualToDefaultInstance();
        SubscriptionObserver observer = new SubscriptionObserver(consumer);
        observer.onNext(SubscriptionUpdate.getDefaultInstance());

        SubscriptionUpdate update = GivenSubscriptionUpdate.withTwoEntities();
        assertThrows(AssertionError.class, () -> observer.onNext(update));
    }

    @Test
    @DisplayName("expose mutable counter of the received updates")
    void exposeUpdateCounter() {
        Consumer<SubscriptionUpdate> consumer = update -> {
            // NO-OP.
        };
        SubscriptionObserver observer = new SubscriptionObserver(consumer);
        VerifyingCounter counter = observer.counter();
        assertThat(counter.value()).isEqualTo(0);

        SubscriptionUpdate update = GivenSubscriptionUpdate.withTwoEntities();
        observer.onNext(update);

        assertThat(counter.value()).isEqualTo(2);
    }
}
