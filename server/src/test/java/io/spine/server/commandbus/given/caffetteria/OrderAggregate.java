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

package io.spine.server.commandbus.given.caffetteria;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.commandbus.CmdBusOrder;
import io.spine.test.commandbus.CmdBusOrderId;
import io.spine.test.commandbus.command.CmdBusAllocateTable;
import io.spine.test.commandbus.event.CmdBusTableAllocated;

public final class OrderAggregate
        extends Aggregate<CmdBusOrderId, CmdBusOrder, CmdBusOrder.Builder> {

    @Assign
    CmdBusTableAllocated handle(CmdBusAllocateTable cmd) {
        return CmdBusTableAllocated
                .newBuilder()
                .setId(cmd.getId())
                .setCaffetteria(cmd.getCaffetteria())
                .setVisitorCount(cmd.getVisitors()
                                    .getCount())
                .build();
    }

    @Apply
    private void on(CmdBusTableAllocated event) {
        builder().setCaffetteria(event.getCaffetteria());
        // For simplicity, the chosen table index is always `0`.
        builder().setTableIndex(0);
    }
}