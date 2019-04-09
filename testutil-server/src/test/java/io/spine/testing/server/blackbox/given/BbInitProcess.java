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

package io.spine.testing.server.blackbox.given;

import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tuple.Pair;
import io.spine.testing.server.blackbox.BbInit;
import io.spine.testing.server.blackbox.BbInitVBuilder;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.command.BbAssignScrumMaster;
import io.spine.testing.server.blackbox.command.BbAssignTeam;
import io.spine.testing.server.blackbox.command.BbInitProject;
import io.spine.testing.server.blackbox.event.BbProjectInitialized;
import io.spine.testing.server.blackbox.event.BbTeamAssigned;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * Test environment process manager for testing
 * {@link io.spine.testing.server.entity.EntitySubject}.
 */
public final class BbInitProcess extends ProcessManager<BbProjectId, BbInit, BbInitVBuilder> {

    BbInitProcess(BbProjectId id) {
        super(id);
    }

    @Command
    Pair<BbAssignTeam, Optional<BbAssignScrumMaster>> on(BbInitProject cmd) {
        builder().setId(cmd.getProjectId());
        BbAssignTeam assignTeam = BbAssignTeam
                .vBuilder()
                .setProjectId(cmd.getProjectId())
                .addAllMember(cmd.getMemberList())
                .build();
        @Nullable BbAssignScrumMaster assignScrumMaster =
                cmd.hasScrumMaster()
                ? BbAssignScrumMaster
                        .vBuilder()
                        .setProjectId(cmd.getProjectId())
                        .setScrumMaster(cmd.getScrumMaster())
                        .build()
                : null;
        return Pair.withNullable(assignTeam, assignScrumMaster);
    }

    @Assign
    Pair<BbTeamAssigned, BbProjectInitialized> on(BbAssignTeam cmd) {
        builder()
                .setId(cmd.getProjectId())
                .setInitialized(true);
        setDeleted(true);
        return Pair.of(
                BbTeamAssigned
                        .vBuilder()
                        .setProjectId(cmd.getProjectId())
                        .addAllMember(cmd.getMemberList())
                        .build(),
                BbProjectInitialized
                        .vBuilder()
                        .setProjectId(cmd.getProjectId())
                        .build()
        );
    }
}
