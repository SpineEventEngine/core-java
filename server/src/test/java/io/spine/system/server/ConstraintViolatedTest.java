/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.system.server;

import io.spine.core.UserId;
import io.spine.net.EmailAddress;
import io.spine.server.BoundedContextBuilder;
import io.spine.system.server.given.diagnostics.ValidatedAggregate;
import io.spine.system.server.given.diagnostics.VerificationProcman;
import io.spine.system.server.given.diagnostics.ViolationsWatch;
import io.spine.system.server.test.InvalidText;
import io.spine.system.server.test.StartVerification;
import io.spine.system.server.test.ValidateAndSet;
import io.spine.system.server.test.ValidatedId;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.system.server.given.diagnostics.ViolationsWatch.DEFAULT;

@DisplayName("ConstraintViolated should be emitted when")
class ConstraintViolatedTest {

    @MuteLogging
    @Test
    @DisplayName("an entity state is set to an invalid value as a result of an event")
    void afterEvent() {
        String invalidText = "123-non numerical";
        BlackBoxContext<?> context = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                    .add(ValidatedAggregate.class)
                    .add(new ViolationsWatch.Repository())
        );
        context.receivesCommand(
                ValidateAndSet.newBuilder()
                              .setId(ValidatedId.generate())
                              .setTextToValidate(invalidText)
                              .vBuild()
        );
        context.assertEntity(ViolationsWatch.class, DEFAULT)
               .hasStateThat()
               .isEqualTo(InvalidText.newBuilder()
                                     .setId(DEFAULT)
                                     .setInvalidText(invalidText)
                                     .buildPartial()
               );
    }

    @MuteLogging
    @Test
    @DisplayName("an entity state is set to an invalid value as a result of a command")
    void afterCommand() {
        BlackBoxContext<?> context = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                .add(VerificationProcman.class)
                .add(new ViolationsWatch.Repository())
        );
        context.receivesCommand(
                StartVerification
                        .newBuilder()
                        .setUserId(UserId.newBuilder()
                                         .setValue(newUuid()))
                        .setAddress(EmailAddress.newBuilder()
                                                .setValue("a@b.c"))
                        .vBuild()
        );
        context.assertEntity(ViolationsWatch.class, DEFAULT)
               .hasStateThat()
               .isEqualTo(InvalidText.newBuilder()
                                     .setId(DEFAULT)
                                     .setErrorMessage("A value must be set.")
                                     .buildPartial());
    }
}
