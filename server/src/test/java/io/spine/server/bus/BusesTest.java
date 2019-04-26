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

package io.spine.server.bus;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.commandbus.command.CmdBusStartProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;

@DisplayName("Buses utility should")
class BusesTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Buses.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        Command command = Command
                .newBuilder()
                .setMessage(pack(CmdBusStartProject.getDefaultInstance()))
                .build();
        CannotModifyArchivedEntity throwable = CannotModifyArchivedEntity
                .newBuilder()
                .setEntityId(Any.getDefaultInstance())
                .build();
        throwable.initProducer(Any.getDefaultInstance());
        RejectionEnvelope defaultRejection = RejectionEnvelope.from(
                CommandEnvelope.of(command),
                throwable
        );
        new NullPointerTester()
                .setDefault(Message.class, Any.getDefaultInstance())
                .setDefault(Error.class, Error.newBuilder()
                                              .setCode(1)
                                              .build())
                .setDefault(RejectionEnvelope.class, defaultRejection)
                .testAllPublicStaticMethods(Buses.class);
    }
}
