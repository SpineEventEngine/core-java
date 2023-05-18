/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.server.delivery.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.test.delivery.AddNumber;
import io.spine.test.delivery.Calc;
import io.spine.test.delivery.NumberAdded;
import io.spine.test.delivery.NumberImported;
import io.spine.test.delivery.NumberReacted;

/**
 * A calculator that is only capable of adding integer numbers.
 */
public class CalcAggregate extends Aggregate<String, Calc, Calc.Builder> {

    @Assign
    NumberAdded handle(AddNumber command) {
        int value = command.getValue();
        return NumberAdded.newBuilder()
                          .setValue(value)
                          .vBuild();
    }

    @Apply
    private void on(NumberAdded event) {
        int currentSum = builder().getSum();
        builder().setSum(currentSum + event.getValue());
    }

    @Apply(allowImport = true)
    private void on(NumberImported event) {
        int currentSum = builder().getSum();
        builder().setSum(currentSum + event.getValue());
    }

    @React
    NumberAdded on(NumberReacted event) {
        return NumberAdded
                .newBuilder()
                .setCalculatorId(event.getCalculatorId())
                .setValue(event.getValue())
                .vBuild();
    }
}
