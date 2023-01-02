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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import io.spine.base.Error;
import io.spine.server.delivery.given.ReceptionFailureTestEnv.MarkFailureDeliveredMonitor;
import io.spine.server.delivery.given.ReceptionFailureTestEnv.ObservingMonitor;
import io.spine.test.delivery.command.TurnConditionerOn;
import io.spine.testing.SlowTest;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.blackBox;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.configureDelivery;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.inboxMessages;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.receptionist;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.sleep;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.tellToTurnConditioner;
import static io.spine.server.delivery.given.ReceptionistAggregate.FAILURE_MESSAGE;
import static io.spine.server.delivery.given.ReceptionistAggregate.makeApplierFail;
import static io.spine.server.delivery.given.ReceptionistAggregate.makeApplierPass;

@SlowTest
@DisplayName("`Delivery` should allow to monitor the failed reception of signals ")
@SuppressWarnings("resource")   /* We don't care about closing black boxes in this test. */
final class ReceptionFailureTest extends AbstractDeliveryTest {

    @Test
    @DisplayName("and repeat dispatching of the corresponding `InboxMessage`")
    @MuteLogging
    void allowFailureRethrow() {
        ObservingMonitor monitor = new ObservingMonitor();
        configureDelivery(monitor);
        BlackBoxContext context = blackBox().tolerateFailures();

        String receptionistId = newUuid();
        TurnConditionerOn command = tellToTurnConditioner(receptionistId);
        makeApplierPass();
        context.receivesCommand(command);
        sleep();
        assertThat(monitor.lastFailure()).isEmpty();
        context.assertState(receptionistId, receptionist(receptionistId, 1));

        AtomicBoolean failureObserved = new AtomicBoolean(false);
        monitor.setResolver((failure) -> {
            failureObserved.set(true);
            Error error = failure.error();
            assertThat(error.getStacktrace()).contains(FAILURE_MESSAGE);

            makeApplierPass();
            return failure.repeatDispatching();
        });

        makeApplierFail();
        context.receivesCommand(command);
        sleep();

        assertThat(failureObserved.get())
                .isTrue();
        assertInboxEmpty();
    }

    @Test
    @DisplayName("and mark the corresponding `InboxMessage` as delivered")
    @MuteLogging
    void allowMarkingFailedMessageAsDelivered() {
        MarkFailureDeliveredMonitor monitor = new MarkFailureDeliveredMonitor();
        configureDelivery(monitor);
        BlackBoxContext context = blackBox().tolerateFailures();

        String receptionistId = newUuid();
        TurnConditionerOn command = tellToTurnConditioner(receptionistId);
        makeApplierFail();
        context.receivesCommand(command);
        sleep();

        assertThat(monitor.failureReceived()).isTrue();
        assertInboxEmpty();
    }

    private static void assertInboxEmpty() {
        ImmutableList<InboxMessage> messages = inboxMessages();
        assertThat(messages.size()).isEqualTo(0);
    }
}
