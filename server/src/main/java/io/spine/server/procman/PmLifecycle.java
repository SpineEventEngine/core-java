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

import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.server.procman.model.Lifecycle;

/**
 * Lifecycle of a {@link ProcessManager} instance.
 */
final class PmLifecycle {

    private final ProcessManager processManager;
    private final Lifecycle pmClassLifecycle;

    /**
     * Creates a new instance for the given process manager and its lifecycle rules.
     *
     * @param processManager
     *         the process manager instance
     * @param pmClassLifecycle
     *         the PM class lifecycle rules
     */
    PmLifecycle(ProcessManager processManager, Lifecycle pmClassLifecycle) {
        this.processManager = processManager;
        this.pmClassLifecycle = pmClassLifecycle;
    }

    /**
     * Updates the process manager lifecycle according to the given events.
     *
     * <p>This method should only be called in a scope of active transaction.
     */
    void update(Iterable<Event> events) {
        if (pmClassLifecycle.archivesUpon(events)) {
            processManager.setArchived(true);
        }
        if (pmClassLifecycle.deletesUpon(events)) {
            processManager.setDeleted(true);
        }
    }

    /**
     * Updates the process manager lifecycle according to the given rejection.
     *
     * <p>This method should only be called in a scope of active transaction.
     */
    void update(ThrowableMessage rejection) {
        if (pmClassLifecycle.archivesUpon(rejection)) {
            processManager.setArchived(true);
        }
        if (pmClassLifecycle.deletesUpon(rejection)) {
            processManager.setDeleted(true);
        }
    }
}
