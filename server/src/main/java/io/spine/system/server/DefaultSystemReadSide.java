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

import io.spine.annotation.Internal;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The default implementation of a {@link SystemReadSide}.
 *
 * @see SystemReadSide#newInstance(SystemContext)
 */
@Internal
final class DefaultSystemReadSide implements SystemReadSide {

    private final SystemContext context;
    private final EventBus eventBus;

    DefaultSystemReadSide(SystemContext context) {
        this.context = context;
        this.eventBus = context.eventBus();
    }

    @Override
    public void register(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        eventBus.register(dispatcher);
    }

    @Override
    public void unregister(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        eventBus.unregister(dispatcher);
    }

    @Override
    public Iterator<EntityStateWithVersion> readDomainAggregate(Query query) {
        MirrorRepository repository = (MirrorRepository)
                context.findRepository(Mirror.class)
                       .orElseThrow(
                               () -> newIllegalStateException(
                                       "Mirror projection repository is not registered in `%s`.",
                                       context.name()
                                              .getValue()
                               )
                       );
        Iterator<EntityStateWithVersion> result = repository.execute(query);
        return result;
    }
}
