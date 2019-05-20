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

package io.spine.testing.server.projection.given.prj;

import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.testing.server.given.entity.TuEventLog;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.event.TuProjectAssigned;
import io.spine.testing.server.given.entity.event.TuProjectCreated;

import static io.spine.protobuf.AnyPacker.pack;

/**
 * A sample projection for being used as the subject of tests under tests.
 */
public class TuEventLoggingView
        extends Projection<TuProjectId, TuEventLog, TuEventLog.Builder> {

    public TuEventLoggingView(TuProjectId id) {
        super(id);
    }

    @Subscribe
    void on(TuProjectAssigned event) {
        builder().addEvent(pack(event));
    }

    @Subscribe
    void on(TuProjectCreated event) {
        builder().addEvent(pack(event));
    }
}
