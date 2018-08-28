/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import static io.spine.type.TypeUrl.parse;

/**
 * @author Dmytro Dashenkov
 */
final class DuplicateGuard {

    private static final TypeUrl COMMAND_DISPATCHED_EVENT_TYPE =
            TypeUrl.from(CommandDispatchedToHandler.getDescriptor());

    private static final TypeUrl EVENT_DISPATCHED_TO_REACTOR_EVENT_TYPE =
            TypeUrl.from(EventDispatchedToReactor.getDescriptor());

    private static final TypeUrl EVENT_DISPATCHED_TO_SUBSCRIBER_EVENT_TYPE =
            TypeUrl.from(EventDispatchedToSubscriber.getDescriptor());

    private final RecentHistory history;

    private DuplicateGuard(RecentHistory history) {
        this.history = history;
    }

    static DuplicateGuard atopOf(RecentHistory history) {
        checkNotNull(history);
        return new DuplicateGuard(history);
    }

    boolean isDuplicate(Event candidate) {
        return isDuplicateOfReact(candidate) || isDuplicateOfSubscribe(candidate);
    }

    private boolean isDuplicateOfReact(Event candidate) {
        EventId candidateId = candidate.getId();
        boolean duplicate = history.stream()
                                   .filter(DuplicateGuard::isEventDispatchedToReactor)
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
                                   .filter(DuplicateGuard::isEventDispatchedToSubscriber)
                                   .map(Events::getMessage)
                                   .map(EventDispatchedToSubscriber.class::cast)
                                   .map(EventDispatchedToSubscriber::getPayload)
                                   .map(Event::getId)
                                   .anyMatch(candidateId::equals);
        return duplicate;
    }

    boolean isDuplicate(Command candidate) {
        CommandId candidateId = candidate.getId();
        boolean duplicate = history.stream()
                                   .filter(DuplicateGuard::isCommandDispatchedToHanlder)
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

    private static boolean isCommandDispatchedToHanlder(Event candidate) {
        return instanceOf(candidate, COMMAND_DISPATCHED_EVENT_TYPE);
    }

    private static boolean instanceOf(Event event, TypeUrl type) {
        String typeRaw = event.getMessage().getTypeUrl();
        TypeUrl actualType = parse(typeRaw);
        return type.equals(actualType);
    }
}
