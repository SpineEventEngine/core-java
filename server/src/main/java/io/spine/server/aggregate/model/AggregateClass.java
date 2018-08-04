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

package io.spine.server.aggregate.model;

import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.model.CommandHandlingEntityClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.event.model.ReactorClass;
import io.spine.server.event.model.ReactorClassDelegate;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.rejection.model.RejectionReactorMethod;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides message handling information on an aggregate class.
 *
 * @param <A> the type of aggregates
 * @author Alexander Yevsyukov
 */
public class AggregateClass<A extends Aggregate>
        extends CommandHandlingEntityClass<A>
        implements ReactorClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventApplier> stateEvents;
    private final ReactorClassDelegate<A> delegate;

    /** Creates new instance. */
    protected AggregateClass(Class<A> cls) {
        super(checkNotNull(cls));
        this.stateEvents = new MessageHandlerMap<>(cls, EventApplier.factory());
        this.delegate = new ReactorClassDelegate<>(cls);
    }

    /**
     * Obtains an aggregate class for the passed raw class.
     */
    public static <A extends Aggregate> AggregateClass<A> asAggregateClass(Class<A> cls) {
        checkNotNull(cls);
        AggregateClass<A> result = (AggregateClass<A>)
                get(cls, AggregateClass.class, () -> new AggregateClass<>(cls));
        return result;
    }

    @Override
    public Set<EventClass> getEventClasses() {
        return delegate.getEventClasses();
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return delegate.getExternalEventClasses();
    }

    @Override
    public Set<RejectionClass> getRejectionClasses() {
        return delegate.getRejectionClasses();
    }

    @Override
    public Set<RejectionClass> getExternalRejectionClasses() {
        return delegate.getExternalRejectionClasses();
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass) {
        return delegate.getReactor(eventClass);
    }

    @Override
    public RejectionReactorMethod getReactor(RejectionClass rejCls, CommandClass cmdCls) {
        return delegate.getReactor(rejCls, cmdCls);
    }

    public EventApplier getApplier(EventClass eventClass) {
        return stateEvents.getMethod(eventClass);
    }
}
