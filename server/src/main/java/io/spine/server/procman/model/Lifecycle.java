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

package io.spine.server.procman.model;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.code.proto.MessageType;
import io.spine.core.Event;
import io.spine.core.EventClassSet;
import io.spine.option.LifecycleOption;

import java.io.Serializable;
import java.util.Optional;

/**
 * Lifecycle of a process manager class as represented in the domain model.
 */
@Internal
@Immutable
public final class Lifecycle implements Serializable {

    private static final long serialVersionUID = 0L;

    private final EventClassSet archiveOn;
    private final EventClassSet deleteOn;

    private Lifecycle(EventClassSet archiveOn, EventClassSet deleteOn) {
        this.archiveOn = archiveOn;
        this.deleteOn = deleteOn;
    }

    private static Lifecycle empty() {
        return new Lifecycle(EventClassSet.empty(), EventClassSet.empty());
    }

    static Lifecycle from(LifecycleOption lifecycleOption) {
        EventClassSet.parse(lifecycleOption.getArchiveUpon());
        EventClassSet.parse(lifecycleOption.getDeleteUpon());
        return null;
    }

    public static Lifecycle of(Class<? extends Message> stateClass) {
        MessageType type = MessageType.of(stateClass);
        Optional<LifecycleOption> option = io.spine.code.proto.Lifecycle.lifecycleOf(type);
        if (option.isPresent()) {
            return from(option.get());
        }
        return empty();
    }

    public boolean archivesOn(Iterable<Event> events) {
        return archiveOn.containsAnyOf(events);
    }

    public boolean deletesOn(Iterable<Event> events) {
        return deleteOn.containsAnyOf(events);
    }
}
