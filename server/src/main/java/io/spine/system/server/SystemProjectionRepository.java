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

import com.google.protobuf.Message;
import io.spine.core.EventEnvelope;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A repository for projections in a system bounded context.
 *
 * <p>Unlike an arbitrary {@link ProjectionRepository}, a {@code SystemProjectionRepository}
 * dispatches the events directly to the target projections.
 *
 * @author Dmytro Dashenkov
 */
public class SystemProjectionRepository<I, P extends Projection<I, S, ?>, S extends Message>
        extends ProjectionRepository<I, P, S> {

    /**
     * {@inheritDoc}
     *
     * @implNote
     * A {@code SystemProjectionRepository} dispatches the given event directly to its targets,
     * whereas a domain repository would send a command to the system context.
     */
    @Override
    public final Set<I> dispatch(EventEnvelope envelope) {
        checkNotNull(envelope);
        Set<I> ids = route(envelope);
        ids.forEach(id -> dispatchNowTo(id, envelope));
        return ids;
    }
}
