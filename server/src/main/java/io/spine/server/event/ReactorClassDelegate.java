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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.ModelClass;
import io.spine.server.rejection.RejectionReactorMethod;

import java.util.Set;

import static io.spine.server.model.HandlerMethod.domestic;
import static io.spine.server.model.HandlerMethod.external;

/**
 * The helper class for holding messaging information on behalf of another model class.
 *
 * @param <T> the type of the raw class for obtaining messaging information
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
@Internal
public final class ReactorClassDelegate<T> extends ModelClass<T> implements ReactorClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventReactorMethod> eventReactions;
    private final MessageHandlerMap<RejectionClass, RejectionReactorMethod> rejectionReactions;

    private final ImmutableSet<EventClass> domesticEventReactions;
    private final ImmutableSet<EventClass> externalEventReactions;
    private final ImmutableSet<RejectionClass> domesticRejectionReactions;
    private final ImmutableSet<RejectionClass> externalRejectionReactions;

    public ReactorClassDelegate(Class<T> cls) {
        super(cls);
        this.eventReactions = new MessageHandlerMap<>(cls, EventReactorMethod.factory());
        this.rejectionReactions = new MessageHandlerMap<>(cls, RejectionReactorMethod.factory());

        this.domesticEventReactions = eventReactions.getMessageClasses(domestic());
        this.externalEventReactions = eventReactions.getMessageClasses(external());

        this.domesticRejectionReactions = rejectionReactions.getMessageClasses(domestic());
        this.externalRejectionReactions = rejectionReactions.getMessageClasses(external());
    }

    @Override
    public Set<EventClass> getEventReactions() {
        return domesticEventReactions;
    }

    @Override
    public Set<EventClass> getExternalEventReactions() {
        return externalEventReactions;
    }

    @Override
    public Set<RejectionClass> getRejectionReactions() {
        return domesticRejectionReactions;
    }

    @Override
    public Set<RejectionClass> getExternalRejectionReactions() {
        return externalRejectionReactions;
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass) {
        return eventReactions.getMethod(eventClass);
    }

    @Override
    public RejectionReactorMethod getReactor(RejectionClass rejCls, CommandClass cmdCls) {
        return rejectionReactions.getMethod(rejCls, cmdCls);
    }
}
