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

package io.spine.server.entity;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.core.Event;
import io.spine.core.Events;
import io.spine.system.server.SystemEventFactory;
import io.spine.system.server.SystemGateway;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
final class FilteringGateway implements SystemGateway {

    private final SystemGateway delegate;
    private final EventFilter filter;

    private FilteringGateway(SystemGateway delegate, EventFilter filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    static SystemGateway atopOf(SystemGateway delegate, EventFilter filter) {
        checkNotNull(delegate);
        checkNotNull(filter);
        return new FilteringGateway(delegate, filter);
    }

    @Override
    public void postCommand(Message systemCommand) {
        delegate.postCommand(systemCommand);
    }

    @Override
    public void postEvent(Message systemEvent) {
        SystemEventFactory eventFactory = SystemEventFactory.forMessage(systemEvent, false);
        Event event = eventFactory.createEvent(systemEvent, null);
        Optional<Event> filtered = this.filter.filter(event);
        filtered.map(Events::getMessage)
                .ifPresent(delegate::postEvent);
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        return delegate.readDomainAggregate(query);
    }
}
