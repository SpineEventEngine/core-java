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

package io.spine.server.rejection.given;

import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.server.rejection.DispatcherRejectionDelivery;
import io.spine.server.rejection.RejectionDispatcher;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.collect.Maps.newHashMap;

public class PostponedDispatcherRejectionDelivery extends DispatcherRejectionDelivery {

    private final Map<RejectionEnvelope,
            Class<? extends RejectionDispatcher>> postponedExecutions = newHashMap();

    public PostponedDispatcherRejectionDelivery(Executor delegate) {
        super(delegate);
    }

    @Override
    public boolean shouldPostponeDelivery(RejectionEnvelope failure,
                                          RejectionDispatcher<?> consumer) {
        postponedExecutions.put(failure, consumer.getClass());
        return true;
    }

    public boolean isPostponed(Rejection rejection, RejectionDispatcher<?> dispatcher) {
        final RejectionEnvelope envelope = RejectionEnvelope.of(rejection);
        final Class<? extends RejectionDispatcher> actualClass = postponedExecutions.get(
                envelope);
        final boolean rejectionPostponed = actualClass != null;
        final boolean dispatcherMatches = rejectionPostponed && dispatcher.getClass()
                                                                          .equals(actualClass);
        return dispatcherMatches;
    }

    public Set<RejectionEnvelope> getPostponedRejections() {
        final Set<RejectionEnvelope> envelopes = postponedExecutions.keySet();
        return envelopes;
    }
}
