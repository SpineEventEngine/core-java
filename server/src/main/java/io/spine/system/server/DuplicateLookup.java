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

package io.spine.system.server;

import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.Events;
import io.spine.server.entity.RecentHistory;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Performs a lookup over a given recent history to tell whether or not a given message has already
 * been dispatched to the given entity.
 */
final class DuplicateLookup {

    private static final TypeUrl COMMAND_DISPATCHED_EVENT_TYPE =
            TypeUrl.from(CommandDispatchedToHandler.getDescriptor());

    private static final TypeUrl EVENT_DISPATCHED_TO_REACTOR_EVENT_TYPE =
            TypeUrl.from(EventDispatchedToReactor.getDescriptor());

    private static final TypeUrl EVENT_DISPATCHED_TO_SUBSCRIBER_EVENT_TYPE =
            TypeUrl.from(EventDispatchedToSubscriber.getDescriptor());

    private final RecentHistory history;

    private DuplicateLookup(RecentHistory history) {
        this.history = history;
    }

    /**
     * Creates a new lookup through the given {@link RecentHistory}.
     *
     * @param history the history of entity to search for duplicates in
     * @return new instance of {@code DuplicateLookup}
     */
    static DuplicateLookup through(RecentHistory history) {
        checkNotNull(history);
        return new DuplicateLookup(history);
    }

    /**
     * Tells if the given event has already been dispatched to the given entity.
     *
     * @param candidate
     *         the event to check
     * @return {@code true} if the given event is a duplicate, {@code false} otherwise
     */
    boolean isDuplicate(Event candidate) {
        return isDuplicateOfReact(candidate) || isDuplicateOfSubscribe(candidate);
    }

    private boolean isDuplicateOfReact(Event candidate) {
        EventId candidateId = candidate.getId();
        boolean duplicate = history.stream()
                                   .filter(DuplicateLookup::isEventDispatchedToReactor)
                                   .map(Events::getMessage)
                                   .map(EventDispatchedToReactor.class::cast)
                                   .map(EventDispatchedToReactor::getPayload)
                                   .map(Event::getId)
                                   .anyMatch(candidateId::equals);
        return duplicate;
    }

    private boolean isDuplicateOfSubscribe(Event candidate) {
        EventId candidateId = candidate.getId();
        boolean duplicate = history.stream()
                                   .filter(DuplicateLookup::isEventDispatchedToSubscriber)
                                   .map(Events::getMessage)
                                   .map(EventDispatchedToSubscriber.class::cast)
                                   .map(EventDispatchedToSubscriber::getPayload)
                                   .map(Event::getId)
                                   .anyMatch(candidateId::equals);
        return duplicate;
    }

    /**
     * Tells if the given command has already been dispatched to the given entity.
     *
     * @param candidate
     *         the command to check
     * @return {@code true} if the given command is a duplicate, {@code false} otherwise
     */
    boolean isDuplicate(Command candidate) {
        CommandId candidateId = candidate.getId();
        boolean duplicate = history.stream()
                                   .filter(DuplicateLookup::isCommandDispatchedToHandler)
                                   .map(Events::getMessage)
                                   .map(CommandDispatchedToHandler.class::cast)
                                   .map(CommandDispatchedToHandler::getPayload)
                                   .map(Command::getId)
                                   .anyMatch(candidateId::equals);
        return duplicate;
    }

    private static boolean isEventDispatchedToSubscriber(Event candidate) {
        return instanceOf(candidate, EVENT_DISPATCHED_TO_SUBSCRIBER_EVENT_TYPE);
    }

    private static boolean isEventDispatchedToReactor(Event candidate) {
        return instanceOf(candidate, EVENT_DISPATCHED_TO_REACTOR_EVENT_TYPE);
    }

    private static boolean isCommandDispatchedToHandler(Event candidate) {
        return instanceOf(candidate, COMMAND_DISPATCHED_EVENT_TYPE);
    }

    private static boolean instanceOf(Event event, TypeUrl type) {
        TypeUrl actualType = event.typeUrl();
        return type.equals(actualType);
    }
}
