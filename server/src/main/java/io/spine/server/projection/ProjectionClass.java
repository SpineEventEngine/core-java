/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.projection;

import io.spine.annotation.Internal;
import io.spine.core.EventClass;
import io.spine.server.event.EventSubscriberMethod;
import io.spine.server.model.EntityClass;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.MethodFiltering;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides type information on a projection class.
 *
 * @param <P> the type of projections
 * @author Alexander Yevsyukov
 */
@Internal
public final class ProjectionClass<P extends Projection> extends EntityClass<P> {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventSubscriberMethod> eventSubscriptions;

    private ProjectionClass(Class<? extends P> cls) {
        super(cls);
        this.eventSubscriptions = new MessageHandlerMap<>(cls, EventSubscriberMethod.factory());
    }

    public static <P extends Projection> ProjectionClass<P> of(Class<P> cls) {
        checkNotNull(cls);
        return new ProjectionClass<>(cls);
    }

    Set<EventClass> getEventSubscriptions() {
        return eventSubscriptions.getMessageClasses();
    }

    Set<EventClass> getExternalEventSubscriptions() {
        return eventSubscriptions.getMessageClasses(
                MethodFiltering.<EventSubscriberMethod>onlyExternal());
    }

    EventSubscriberMethod getSubscriber(EventClass eventClass) {
        return eventSubscriptions.getMethod(eventClass);
    }
}
