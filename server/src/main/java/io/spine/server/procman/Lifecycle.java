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

import com.google.common.annotations.VisibleForTesting;
import io.spine.base.EventMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.type.EventClass;
import io.spine.server.type.RejectionClass;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.asList;

/**
 * Lifecycle of a process manager class as represented in the domain model.
 *
 * <p>Lists events and rejections which are "terminal" for the process, causing the process manager
 * to become {@linkplain LifecycleFlags#getArchived()} archived} or
 * {@linkplain LifecycleFlags#getDeleted()} deleted}.
 *
 * @see io.spine.option.LifecycleOption
 */
public final class Lifecycle implements Serializable {

    private static final long serialVersionUID = 0L;

    private final Set<Class<? extends EventMessage>> archiveOn;
    private final Set<Class<? extends EventMessage>> deleteOn;

    @VisibleForTesting
    public Lifecycle() {
        this.archiveOn = new HashSet<>();
        this.deleteOn = new HashSet<>();
    }

    @SafeVarargs
    public final void archiveOn(Class<? extends EventMessage>... messageClasses) {
        archiveOn.addAll(asList(messageClasses));
    }

    /**
     * Checks if the process manager should become archived when the given {@code events} are
     * emitted.
     */
    boolean archivesOn(Iterable<Event> events) {
        boolean result = containsAnyClasses(archiveOn, events);
        return result;
    }

    /**
     * Checks if the process manager should become archived when the given {@code rejection} is
     * thrown.
     */
    boolean archivesOn(ThrowableMessage rejection) {
        RejectionClass rejectionClass = RejectionClass.of(rejection);
        boolean result = archiveOn.contains(rejectionClass.value());
        return result;
    }

    @SafeVarargs
    public final void deleteOn(Class<? extends EventMessage>... messageClasses) {
        deleteOn.addAll(asList(messageClasses));
    }

    /**
     * Checks if the process manager should become deleted when the given {@code events} are
     * emitted.
     */
    boolean deletesOn(Iterable<Event> events) {
        boolean result = containsAnyClasses(deleteOn, events);
        return result;
    }

    /**
     * Checks if the process manager should become deleted when the given {@code rejection} is
     * thrown.
     */
    boolean deletesOn(ThrowableMessage rejection) {
        RejectionClass rejectionClass = RejectionClass.of(rejection);
        boolean result = deleteOn.contains(rejectionClass.value());
        return result;
    }

    private static boolean containsAnyClasses(Set<Class<? extends EventMessage>> classSet,
                                              Iterable<Event> events) {
        boolean result =
                stream(events)
                        .map(EventClass::from)
                        .map(EventClass::value)
                        .anyMatch(classSet::contains);
        return result;
    }
}
