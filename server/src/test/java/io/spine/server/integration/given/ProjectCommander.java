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

package io.spine.server.integration.given;

import io.spine.core.External;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Command;
import io.spine.test.integration.command.ItgAddTask;
import io.spine.test.integration.command.ItgStartProject;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;

import java.util.Optional;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
public final class ProjectCommander extends AbstractCommander {

    private static ItgProjectCreated externalEvent = null;
    private static ItgProjectStarted domesticEvent = null;


    @Command
    Optional<ItgStartProject> on(@External ItgProjectCreated event) {
        externalEvent = event;
        return Optional.empty();
    }

    @Command
    Optional<ItgAddTask> on(ItgProjectStarted event) {
        domesticEvent = event;
        return Optional.empty();
    }

    public static ItgProjectCreated externalEvent() {
        return externalEvent;
    }

    public static ItgProjectStarted domesticEvent() {
        return domesticEvent;
    }

    public static void clear() {
        externalEvent = null;
        domesticEvent = null;
    }
}
