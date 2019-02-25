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
import io.spine.base.ThrowableMessage;
import io.spine.code.proto.EntityLifecycleOption;
import io.spine.core.Event;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.type.EventClassSet;
import io.spine.type.MessageType;

import java.io.Serializable;

/**
 * Lifecycle of a process manager class as represented in the domain model.
 *
 * <p>Lists events and rejections which are "terminal" for the process, causing the process manager
 * to become {@linkplain LifecycleFlags#getArchived()} archived} or
 * {@linkplain LifecycleFlags#getDeleted()} deleted}.
 *
 * @see io.spine.option.LifecycleOption
 */
@Immutable
public final class Lifecycle implements Serializable {

    private static final long serialVersionUID = 0L;

    private final EventClassSet archiveOn;
    private final EventClassSet deleteOn;

    private Lifecycle(EventClassSet archiveOn, EventClassSet deleteOn) {
        this.archiveOn = archiveOn;
        this.deleteOn = deleteOn;
    }

    /**
     * Parses the lifecycle {@linkplain io.spine.option.LifecycleOption options} of a process
     * manager type to create a new {@code Lifecycle} instance.
     */
    public static Lifecycle of(MessageType type) {
        EntityLifecycleOption option = new EntityLifecycleOption();
        EventClassSet archiveUpon =
                option.archiveUpon(type)
                      .map(EventClassSet::parse)
                      .orElseGet(EventClassSet::empty);

        EventClassSet deleteUpon =
                option.deleteUpon(type)
                      .map(EventClassSet::parse)
                      .orElseGet(EventClassSet::empty);
        return new Lifecycle(archiveUpon, deleteUpon);
    }

    /**
     * Creates a new {@code Lifecycle} instance for the given process manager state class.
     */
    public static Lifecycle of(Class<? extends Message> stateClass) {
        MessageType type = new MessageType(stateClass);
        return of(type);
    }

    /**
     * Checks if the process manager should become archived when the given {@code events} are
     * emitted.
     */
    public boolean archivesUpon(Iterable<Event> events) {
        return archiveOn.containsAnyOf(events);
    }

    /**
     * Checks if the process manager should become deleted when the given {@code events} are
     * emitted.
     */
    public boolean deletesUpon(Iterable<Event> events) {
        return deleteOn.containsAnyOf(events);
    }

    /**
     * Checks if the process manager should become archived when the given {@code rejection} is
     * thrown.
     */
    public boolean archivesUpon(ThrowableMessage rejection) {
        return archiveOn.contains(rejection);
    }

    /**
     * Checks if the process manager should become deleted when the given {@code rejection} is
     * thrown.
     */
    public boolean deletesUpon(ThrowableMessage rejection) {
        return deleteOn.contains(rejection);
    }
}
