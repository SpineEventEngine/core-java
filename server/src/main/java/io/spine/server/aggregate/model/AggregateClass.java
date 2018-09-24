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

import com.google.common.collect.ImmutableSet;
import io.spine.core.EventClass;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.model.CommandHandlingEntityClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.event.model.ReactingClass;
import io.spine.server.event.model.ReactorClassDelegate;
import io.spine.server.model.MessageHandlerMap;
import io.spine.type.MessageClass;

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
        implements ReactingClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventApplier> stateEvents;
    private final ImmutableSet<EventClass> importableEvents;
    private final ReactorClassDelegate<A> delegate;

    /** Creates new instance. */
    protected AggregateClass(Class<A> cls) {
        super(checkNotNull(cls));
        this.stateEvents = MessageHandlerMap.create(cls, new EventApplierSignature());
        this.importableEvents = stateEvents.getMessageClasses(EventApplier::allowsImport);
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
    public final Set<EventClass> getEventClasses() {
        return delegate.getEventClasses();
    }

    @Override
    public final Set<EventClass> getExternalEventClasses() {
        return delegate.getExternalEventClasses();
    }

    /**
     * Obtains set of classes of events used as arguments of applier methods.
     *
     * @see #getImportableEventClasses()
     */
    public final Set<EventClass> getStateEventClasses() {
        return stateEvents.getMessageClasses();
    }

    /**
     * Obtains a set of event classes that are
     * {@linkplain io.spine.server.aggregate.Apply#allowImport() imported}
     * by the aggregates of this class.
     *
     * @see #getStateEventClasses()
     */
    public final Set<EventClass> getImportableEventClasses() {
        return importableEvents;
    }

    /**
     * Returns {@code true} if aggregates of this class
     * {@link io.spine.server.aggregate.Apply#allowImport() import} events of at least one class;
     * {@code false} otherwise.
     */
    public final boolean importsEvents() {
        return !importableEvents.isEmpty();
    }

    @Override
    public final EventReactorMethod getReactor(EventClass eventClass, MessageClass commandClass) {
        return delegate.getReactor(eventClass, commandClass);
    }

    /**
     * Obtains event applier method for the passed class of events.
     */
    public final EventApplier getApplier(EventClass eventClass) {
        return stateEvents.getMethod(eventClass);
    }
}
