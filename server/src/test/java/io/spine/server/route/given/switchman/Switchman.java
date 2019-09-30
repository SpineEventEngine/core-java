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

package io.spine.server.route.given.switchman;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.route.given.switchman.command.SetSwitch;
import io.spine.server.route.given.switchman.event.SwitchPositionConfirmed;

/**
 * The aggregate that handles commands send to a switchman.
 */
public final class Switchman extends Aggregate<String, SwitchmanLog, SwitchmanLog.Builder> {

    Switchman(String id) {
        super(id);
    }

    @Assign
    SwitchPositionConfirmed on(SetSwitch cmd) {
        return SwitchPositionConfirmed
                .newBuilder()
                .setSwitchmanName(id())
                .setSwitchId(cmd.getSwitchId())
                .setPosition(cmd.getPosition())
                .build();
    }

    @Apply
    private void event(SwitchPositionConfirmed event) {
        builder()
                .setName(event.getSwitchmanName())
                .setSwitchCount(state().getSwitchCount() + 1);
    }
}