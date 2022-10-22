/*
 * Copyright 2022, TeamDev. All rights reserved.
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
package io.spine.client

import com.google.common.truth.Truth.assertThat
import io.spine.base.CommandMessage
import io.spine.base.Identifier.newUuid
import io.spine.client.given.ActorRequestFactoryTestEnv
import io.spine.client.given.ActorRequestFactoryTestEnv.requestFactoryBuilder
import io.spine.client.given.CommandFactoryTestEnv.INVALID_COMMAND
import io.spine.core.tenantId
import io.spine.test.commands.cmdCreateProject
import io.spine.time.testing.Future
import io.spine.time.testing.Past
import io.spine.validate.ValidationException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Command factory should")
internal class CommandFactorySpec {

    private lateinit var factory: CommandFactory

    @BeforeEach
    fun createFactory() {
        factory = ActorRequestFactoryTestEnv.requestFactory().command()
    }

    @Nested
    internal inner class `create command` {

        /**
         * Tests that a command is created with the current time.
         *
         * @implNote We are creating a range of +/- second between the call to make sure the
         * timestamp would fit into this range. This way the test ensures the sub-second
         * precision of timestamps, which is enough for the purpose of this test.
         */
        @Test
        fun `with current time`() {
            val beforeCall = Past.secondsAgo(1)
            val command = factory.create(command())
            val afterCall = Future.secondsFromNow(1)
            assertTrue(command.isBetween(beforeCall, afterCall))
        }

        @Test
        fun `with given entity version`() {
            val command = factory.create(command(), 2)
            val context = command.context()
            assertThat(context.targetVersion).isEqualTo(2)
        }

        @Test
        fun `with own tenant ID`() {
            val tenantId = tenantId { value = javaClass.simpleName }
            val mtFactory = requestFactoryBuilder().also { f ->
                f.tenantId = tenantId
                f.actor = ActorRequestFactoryTestEnv.ACTOR
            }.build()

            val command = mtFactory.command().create(command())

            assertThat(command.context().actorContext.tenantId)
                .isEqualTo(tenantId)
        }

        private fun command(): CommandMessage = cmdCreateProject { id = newUuid() }
    }

    @Nested
    @DisplayName("throw `ValidationException` when creating command")
    internal inner class NotAccept {

        @Test
        fun `from invalid 'Message'`() {
            assertThrows<ValidationException> {
                factory.create(INVALID_COMMAND)
            }
        }

        @Test
        fun `from invalid 'Message' with version`() {
            assertThrows<ValidationException> {
                factory.create(INVALID_COMMAND, 42)
            }
        }
    }
}
