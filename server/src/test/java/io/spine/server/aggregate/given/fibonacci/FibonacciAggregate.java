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

package io.spine.server.aggregate.given.fibonacci;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.given.fibonacci.command.MoveSequence;
import io.spine.server.aggregate.given.fibonacci.command.SetStartingNumbers;
import io.spine.server.aggregate.given.fibonacci.event.SequenceMoved;
import io.spine.server.aggregate.given.fibonacci.event.StartingNumbersSet;
import io.spine.server.command.Assign;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
// For convenience, to avoid other excessively verbose solutions.
public final class FibonacciAggregate extends Aggregate<SequenceId, Sequence, Sequence.Builder> {

    private static int lastNumberOne = -1;
    private static int lastNumberTwo = -1;

    @Assign
    StartingNumbersSet handle(SetStartingNumbers cmd) {
        StartingNumbersSet event = StartingNumbersSet
                .newBuilder()
                .setId(cmd.getId())
                .setNumberOne(cmd.getNumberOne())
                .setNumberTwo(cmd.getNumberTwo())
                .vBuild();
        return event;
    }

    @Assign
    SequenceMoved handle(MoveSequence cmd) {
        SequenceMoved event = SequenceMoved
                .newBuilder()
                .setId(cmd.getId())
                .vBuild();
        return event;
    }

    @Apply
    private void on(StartingNumbersSet event) {
        int numberOne = event.getNumberOne();
        int numberTwo = event.getNumberTwo();
        builder().setId(event.getId())
                 .setCurrentNumberOne(numberOne)
                 .setCurrentNumberTwo(numberTwo);
        lastNumberOne = numberOne;
        lastNumberTwo = numberTwo;
    }

    @Apply
    private void on(SequenceMoved event) {
        int numberOne = builder().getCurrentNumberOne();
        int numberTwo = builder().getCurrentNumberTwo();
        int sum = numberOne + numberTwo;
        builder().setId(event.getId())
                 .setCurrentNumberOne(numberTwo)
                 .setCurrentNumberTwo(sum);
        lastNumberOne = numberTwo;
        lastNumberTwo = sum;
    }

    public static int lastNumberOne() {
        return lastNumberOne;
    }

    public static int lastNumberTwo() {
        return lastNumberTwo;
    }
}
