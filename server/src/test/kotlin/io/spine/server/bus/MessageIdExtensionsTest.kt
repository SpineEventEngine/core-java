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
package io.spine.server.bus

import com.google.common.truth.Truth.assertThat
import io.spine.base.Error
import io.spine.base.Identifier
import io.spine.core.Ack
import io.spine.core.CommandId
import io.spine.core.Status.StatusCase
import io.spine.protobuf.AnyPacker
import io.spine.server.event.reject
import io.spine.test.bus.ShareId
import io.spine.test.bus.command.ShareCannotBeTraded
import io.spine.testing.client.TestActorRequestFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MessageIdExtensions` should")
internal class MessageIdExtensionsTest {

    @Test
    fun `acknowledge() with OK status`() {
        val ack = ID.acknowledge()
        assertIdEquals(ack)
        assertStatusCase(ack, StatusCase.OK)
    }

    @Test
    fun `reject() with ERROR status`() {
        val error = with(Error.newBuilder()) {
            type = MessageIdExtensionsTest::class.java.canonicalName
            message = "A test error."
            vBuild()
        }
        val ack = ID.causedError(error)
        assertIdEquals(ack)
        assertStatusCase(ack, StatusCase.ERROR)
    }

    @Test
    fun `reject() a command with REJECTION status`() {
        val requestFactory = TestActorRequestFactory(javaClass)
        val command = requestFactory.generateCommand()
        val rejection = ShareCannotBeTraded
            .newBuilder()
            .setShare(
                ShareId.newBuilder()
                    .setValue(Identifier.newUuid())
                    .build()
            )
            .setReason("Ack factory test.")
            .build()
        val re = reject(command, rejection)
        val ack = ID.reject(re)
        assertIdEquals(ack)
        assertStatusCase(ack, StatusCase.REJECTION)
    }

    companion object {

        val ID: CommandId = CommandId.generate()

        private fun assertIdEquals(ack: Ack) {
            val id = AnyPacker.unpack(ack.messageId)
            assertThat(id).isEqualTo(ID)
        }

        private fun assertStatusCase(ack: Ack, status: StatusCase) {
            assertThat(ack.status.statusCase).isEqualTo(status)
        }
    }
}
