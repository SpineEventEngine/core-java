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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.EventMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.type.EventClass;
import io.spine.server.type.RejectionClass;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.asList;

/**
 * The lifecycle rules of a process manager repository.
 *
 * <p>The rules can be configured by user to automatically
 * {@linkplain LifecycleFlags#getArchived()} archive} or {@linkplain LifecycleFlags#getDeleted()}
 * delete} entities when certain events or rejections occur.
 */
public final class LifecycleRules {

    /**
     * Event/rejection classes which will cause the entity to become archived.
     */
    private final Set<Class<? extends EventMessage>> archiveOn;

    /**
     * Event/rejection classes which will cause the entity to become deleted.
     */
    private final Set<Class<? extends EventMessage>> deleteOn;

    @VisibleForTesting
    public LifecycleRules() {
        this.archiveOn = new HashSet<>();
        this.deleteOn = new HashSet<>();
    }

    /**
     * Configures the repository to archive its entities when certain events/rejections occur.
     *
     * <p>Subsequent calls to this method do not clear the previously added event classes,
     * for example, in order not to override each other.
     *
     * @param messageClasses
     *         the event and rejection classes which will cause the entity to become archived
     * @return self for convenient method chaining
     */
    @SafeVarargs
    @CanIgnoreReturnValue
    public final LifecycleRules archiveOn(Class<? extends EventMessage>... messageClasses) {
        archiveOn.addAll(asList(messageClasses));
        return this;
    }

    /**
     * Checks if the process should become archived when the given {@code events} are emitted.
     */
    boolean shouldArchiveOn(Iterable<Event> events) {
        boolean result = containsAnyClasses(archiveOn, events);
        return result;
    }

    /**
     * Checks if the process should become archived when the given {@code rejection} is thrown.
     */
    boolean shouldArchiveOn(ThrowableMessage rejection) {
        RejectionClass rejectionClass = RejectionClass.of(rejection);
        boolean result = archiveOn.contains(rejectionClass.value());
        return result;
    }

    /**
     * Configures the repository to delete its entities when certain events/rejections occur.
     *
     * <p>Subsequent calls to this method do not clear the previously added event classes
     * for example, in order not to override each other.
     *
     * @param messageClasses
     *         the event and rejection classes which will cause the entity to become deleted
     * @return self for convenient method chaining
     */
    @SafeVarargs
    @CanIgnoreReturnValue
    public final LifecycleRules deleteOn(Class<? extends EventMessage>... messageClasses) {
        deleteOn.addAll(asList(messageClasses));
        return this;
    }

    /**
     * Checks if the process should become deleted when the given {@code events} are emitted.
     */
    boolean shouldDeleteOn(Iterable<Event> events) {
        boolean result = containsAnyClasses(deleteOn, events);
        return result;
    }

    /**
     * Checks if the process should become deleted when the given {@code rejection} is thrown.
     */
    boolean shouldDeleteOn(ThrowableMessage rejection) {
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
