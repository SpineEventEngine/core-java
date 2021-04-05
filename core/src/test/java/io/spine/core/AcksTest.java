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

import com.google.protobuf.Empty;
import io.spine.protobuf.AnyPacker;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.core.Acks.toCommandId;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Acks` utility class should")
class AcksTest extends UtilityClassTest<Acks> {

    AcksTest() {
        super(Acks.class);
    }


    @Nested
    @DisplayName("obtain `CommandId`")
    class GetCommandId {

        @Test
        @DisplayName("returning ID value")
        void value() {
            CommandId commandId = CommandId.generate();
            Ack ack = newAck(commandId);
            assertThat(toCommandId(ack))
                    .isEqualTo(commandId);
        }

        @Test
        @DisplayName("throw `IllegalArgumentException`")
        void args() {
            assertThrows(IllegalArgumentException.class, () ->
                    toCommandId(newAck(Events.generateId()))
            );
        }
    }

    /*
     * Test environment
     ************************/

    static Ack newAck(SignalId signalId) {
        return Ack
                .newBuilder()
                .setMessageId(AnyPacker.pack(signalId))
                .setStatus(newOkStatus())
                .build();
    }

    private static Status newOkStatus() {
        return Status
                .newBuilder()
                .setOk(Empty.getDefaultInstance())
                .build();
    }
}
