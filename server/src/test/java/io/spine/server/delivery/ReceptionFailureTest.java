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

import io.spine.base.Identifier;
import io.spine.server.delivery.given.ReceptionFailureTestEnv.ObservingMonitor;
import io.spine.server.delivery.given.ReceptionistAggregate;
import io.spine.test.delivery.command.TurnConditionerOn;
import io.spine.testing.SlowTest;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.blackBox;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.configureDelivery;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.receptionist;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.sleep;
import static io.spine.server.delivery.given.ReceptionFailureTestEnv.tellToTurnConditioner;
import static io.spine.server.delivery.given.ReceptionistAggregate.FAILURE_MESSAGE;

@SlowTest
@DisplayName("`Delivery` should allow to handle the failed reception of signals ")
@SuppressWarnings("resource")   /* We don't care about closing black boxes in this test. */
final class ReceptionFailureTest extends AbstractDeliveryTest {

    @Test
    @DisplayName("via custom delivery monitor with default `rethrow` action")
    void supplyDeliveryMonitor() {
        ObservingMonitor monitor = new ObservingMonitor();
        configureDelivery(monitor);
        BlackBoxContext context = blackBox();

        String receptionistId = Identifier.newUuid();
        TurnConditionerOn command = tellToTurnConditioner(receptionistId);
        context.receivesCommand(command);
        sleep();
        assertThat(monitor.lastFailure()).isEmpty();
        context.assertState(receptionistId, receptionist(receptionistId, 1));

        ReceptionistAggregate.makeApplierFail();
        context.receivesCommand(command);
        sleep();
        assertThat(monitor.lastFailure()).isPresent();
        FailedReception reception = monitor.lastFailure()
                                           .get();
        RuntimeException failure = reception.failure();
        assertThat(failure).isInstanceOf(IllegalStateException.class);
        assertThat(failure).hasMessageThat().contains(FAILURE_MESSAGE);
    }
}
