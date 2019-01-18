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

package io.spine.server.event.given.bus;

import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarted;
import io.spine.testing.server.TestEventFactory;

import static io.spine.server.event.given.bus.EventBusTestEnv.PROJECT_ID;

public class GivenEvent {

    private static final TestEventFactory factory =
            TestEventFactory.newInstance(AnyPacker.pack(PROJECT_ID), GivenEvent.class);

    private GivenEvent() {
    }

    private static TestEventFactory eventFactory() {
        return factory;
    }

    public static Event projectCreated() {
        return projectCreated(PROJECT_ID);
    }

    public static Event projectStarted() {
        ProjectStarted msg = GivenEventMessage.projectStarted();
        Event event = eventFactory().createEvent(msg);
        return event;
    }

    public static Event projectCreated(ProjectId projectId) {
        ProjectCreated msg = GivenEventMessage.projectCreated(projectId);
        Event event = eventFactory().createEvent(msg);
        return event;
    }
}
