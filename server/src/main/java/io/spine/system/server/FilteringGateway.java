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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.Query;
import io.spine.core.Event;
import io.spine.core.Events;
import io.spine.server.entity.EventFilter;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SystemGateway} which applies the given {@link EventFilter} to system events before
 * posting them.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class FilteringGateway implements SystemGateway {

    private final SystemGateway delegate;
    private final EventFilter filter;

    private FilteringGateway(SystemGateway delegate, EventFilter filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    /**
     * Creates a new instance of {@code FilteringGateway} atop of the given gateway delegate and
     * {@link EventFilter} instances.
     *
     * @param delegate
     *         the {@link SystemGateway} which performs all the operations for the resulting gateway
     * @param filter
     *         the {@link EventFilter} to apply to the system events before posting
     * @return new instance of {@code FilteringGateway}
     */
    public static SystemGateway atopOf(SystemGateway delegate, EventFilter filter) {
        checkNotNull(delegate);
        checkNotNull(filter);
        return new FilteringGateway(delegate, filter);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Before posting a system event, applies the given {@link EventFilter} to it. The event is
     * posted only if the filter allows it.
     *
     * <p>If the filter alters the event message, the altered version is posted. The internal event
     * parameters, such as ID, context, etc. are preserved, i.e. if the filter alters them,
     * the <b>original</b> parameters are posted.
     */
    @Override
    public void postEvent(Message systemEvent) {
        SystemEventFactory eventFactory = SystemEventFactory.forMessage(systemEvent, false);
        Event event = eventFactory.createEvent(systemEvent, null);
        Optional<Event> filtered = this.filter.filter(event);
        filtered.map(Events::getMessage)
                .ifPresent(delegate::postEvent);
    }

    @Override
    public void postCommand(Message systemCommand) {
        delegate.postCommand(systemCommand);
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        return delegate.readDomainAggregate(query);
    }
}
