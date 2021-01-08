/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.test.delivery.DTaskAssigned;
import io.spine.test.delivery.DTaskCreated;
import io.spine.test.delivery.DTaskView;

import java.util.Map;

public class TaskView extends Projection<String, DTaskView, DTaskView.Builder> {

    private static final Map<String, DTaskCreated> creationEvents = Maps.newConcurrentMap();

    @Subscribe
    void to(DTaskCreated event) {
        String rawId = event.getId();
        creationEvents.put(rawId, event);
        builder().setId(rawId);
    }

    @Subscribe
    void to(DTaskAssigned event) {
        String rawId = event.getId();
        if (!creationEvents.containsKey(rawId)) {
            throw new IllegalStateException("`DTaskCreated` event was" +
                                                    " not dispatched before `DTaskAssigned`.");
        }
        builder().setAssignee(event.getAssignee());
    }

    public static class Repository extends ProjectionRepository<String, TaskView, DTaskView> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<String> routing) {
            super.setupEventRouting(routing);
            routing.route(DTaskCreated.class,
                          (EventRoute<String, DTaskCreated>) (message, context) ->
                                  ImmutableSet.of(message.getId()));
            routing.route(DTaskAssigned.class,
                          (EventRoute<String, DTaskAssigned>) (message, context) ->
                                  ImmutableSet.of(message.getId()));
        }
    }
}
