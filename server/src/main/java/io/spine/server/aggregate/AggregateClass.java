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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.entity.CommandHandlingEntityClass;
import io.spine.server.event.EventReactorMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.rejection.RejectionReactorMethod;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.HandlerMethod.domestic;
import static io.spine.server.model.HandlerMethod.external;

/**
 * Provides message handling information on an aggregate class.
 *
 * @param <A> the type of aggregates
 * @author Alexander Yevsyukov
 */
@Internal
@SuppressWarnings("ReturnOfCollectionOrArrayField") // impl. is immutable
public class AggregateClass<A extends Aggregate> extends CommandHandlingEntityClass<A> {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventApplier> stateEvents;
    private final MessageHandlerMap<EventClass, EventReactorMethod> eventReactions;
    private final MessageHandlerMap<RejectionClass, RejectionReactorMethod> rejectionReactions;

    private final ImmutableSet<EventClass> domesticEventReactions;
    private final ImmutableSet<EventClass> externalEventReactions;
    private final ImmutableSet<RejectionClass> domesticRejectionReactions;
    private final ImmutableSet<RejectionClass> externalRejectionReactions;

    /** Creates new instance. */
    public AggregateClass(Class<? extends A> cls) {
        super(checkNotNull(cls));
        this.stateEvents = new MessageHandlerMap<>(cls, EventApplier.factory());
        this.eventReactions = new MessageHandlerMap<>(cls, EventReactorMethod.factory());
        this.rejectionReactions = new MessageHandlerMap<>(cls, RejectionReactorMethod.factory());

        this.domesticEventReactions = eventReactions.getMessageClasses(domestic());
        this.externalEventReactions = eventReactions.getMessageClasses(external());

        this.domesticRejectionReactions = rejectionReactions.getMessageClasses(domestic());
        this.externalRejectionReactions = rejectionReactions.getMessageClasses(external());
    }

    Set<EventClass> getEventReactions() {
        return domesticEventReactions;
    }

    Set<EventClass> getExternalEventReactions() {
        return externalEventReactions;
    }

    Set<RejectionClass> getRejectionReactions() {
        return domesticRejectionReactions;
    }

    Set<RejectionClass> getExternalRejectionReactions() {
        return externalRejectionReactions;
    }

    EventApplier getApplier(EventClass eventClass) {
        return stateEvents.getMethod(eventClass);
    }

    EventReactorMethod getReactor(EventClass eventClass) {
        return eventReactions.getMethod(eventClass);
    }

    RejectionReactorMethod getReactor(RejectionClass rejCls, CommandClass cmdCls) {
        return rejectionReactions.getMethod(rejCls, cmdCls);
    }
}
