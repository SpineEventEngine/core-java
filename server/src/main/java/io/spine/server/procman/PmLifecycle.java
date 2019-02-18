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

package io.spine.server.procman;

import io.spine.core.Event;
import io.spine.server.procman.model.Lifecycle;

final class PmLifecycle {

    private final ProcessManager<?, ?, ?> processManager;
    private final Lifecycle lifecycle;

    private PmLifecycle(ProcessManager<?, ?, ?> processManager,
                        Lifecycle lifecycle) {
        this.processManager = processManager;
        this.lifecycle = lifecycle;
    }

    static PmLifecycle of(ProcessManager<?, ?, ?> processManager) {
        Lifecycle lifecycle = processManager.thisClass()
                                            .lifecycle();
        return new PmLifecycle(processManager, lifecycle);
    }

    void updateBasedOn(Iterable<Event> events) {
        archiveIfNecessary(events);
        deleteIfNecessary(events);
    }

    private void archiveIfNecessary(Iterable<Event> events) {
        if (lifecycle.archivesOn(events)) {
            processManager.setArchived(true);
        }
    }

    private void deleteIfNecessary(Iterable<Event> events) {
        if (lifecycle.deletesOn(events)) {
            processManager.setDeleted(true);
        }
    }
}
