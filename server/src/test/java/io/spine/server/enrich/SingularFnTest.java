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

import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import io.spine.core.Enrichment;
import io.spine.core.EventContext;
import io.spine.server.enrich.given.event.SfnTestEvent;
import io.spine.server.enrich.given.event.SfnTestStarted;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SingularFn should")
class SingularFnTest {

    private final TestEventFactory factory = TestEventFactory.newInstance(getClass());

    @Test
    @DisplayName("reject null function")
    void nullFunc() {
        assertThrows(NullPointerException.class, () -> new SingularFn<>(null));
    }

    @Test
    @DisplayName("create Enrichment instance")
    void enrichment() {
        SingularFn<SfnTestEvent, EventContext> fn = new SingularFn<>(
                (m, c) -> c.getTimestamp()
        );

        EventEnvelope event = EventEnvelope.of(
                factory.createEvent(SfnTestStarted.newBuilder()
                                                  .setTestName(getClass().getCanonicalName())
                                                  .build()
                ));

        Enrichment enrichment = fn.apply((SfnTestEvent) event.message(), event.context());
        ProtoSubject<?, Message> assertThat = ProtoTruth.assertThat(enrichment);
        assertThat.isNotNull();
        assertThat.hasAllRequiredFields();
    }
}
