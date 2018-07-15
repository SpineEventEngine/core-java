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
package io.spine.testing.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.protobuf.AnyPacker;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionTransaction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A test utility for dispatching events to a {@code Projection} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class ProjectionEventDispatcher {

    private ProjectionEventDispatcher() {
        // Prevent from instantiation.
    }

    /**
     * Dispatches the {@code Event} to the given {@code Projection}.
     */
    public static void dispatch(Projection<?, ?, ?> projection,
                                Event event) {
        checkNotNull(projection);
        checkNotNull(event);

        final Message unpackedMessage = AnyPacker.unpack(event.getMessage());
        dispatch(projection, unpackedMessage, event.getContext());
    }

    /**
     * Dispatches the passed {@code Event} message along with its context
     * to the given {@code Projection}.
     */
    public static void dispatch(Projection<?, ?, ?> projection,
                                Message eventMessage,
                                EventContext eventContext) {
        checkNotNull(projection);
        checkNotNull(eventMessage);
        checkNotNull(eventContext);

        final ProjectionTransaction<?, ?, ?> tx = ProjectionTransaction.start(projection);
        projection.apply(eventMessage, eventContext);
        tx.commit();
    }
}
