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

package io.spine.server.aggregate;

import com.google.protobuf.Empty;
import io.spine.core.ActorContext;
import io.spine.core.EventContext;
import io.spine.server.type.EmptyClass;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ImportOrigin should")
class ImportOriginTest {

    private final TestActorRequestFactory requestFactory = new TestActorRequestFactory(getClass());

    private ActorContext actorContext;
    private ImportOrigin origin;

    @BeforeEach
    void setUp() {
        actorContext = requestFactory.newActorContext();
        origin = ImportOrigin.newInstance(actorContext);
    }
    @Test
    @DisplayName("set passed ActorContext in EventContext")
    void newInstance() {
        EventContext.Builder builder = EventContext.newBuilder();

        origin.setOriginFields(builder);
        EventContext eventContext = builder.build();

        assertThat(eventContext.getImportContext()).isEqualTo(actorContext);
    }

    @Test
    @DisplayName("return Empty in other methods")
    void emptyValues() {
        Empty empty = Empty.getDefaultInstance();
        assertEquals(empty, origin.getId());
        assertEquals(empty, origin.outerObject());
        assertEquals(empty, origin.message());
        assertEquals(empty, origin.messageContext());
        assertEquals(EmptyClass.instance(), origin.messageClass());
    }
}
