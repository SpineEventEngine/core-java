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

package io.spine.server.trace;

import com.google.common.collect.ImmutableList;
import io.spine.core.MessageId;
import io.spine.core.Signal;
import io.spine.logging.Logging;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

final class CompositeTracer extends AbstractTracer implements Logging {

    private final ImmutableList<Tracer> delegates;

    CompositeTracer(Signal<?, ?, ?> target, ImmutableList<Tracer> delegates) {
        super(target);
        this.delegates = checkNotNull(delegates);
    }

    @Override
    public void processedBy(MessageId receiver) {
        delegates.forEach(tracer -> tracer.processedBy(receiver));
    }

    @Override
    public void close() {
        delegates.forEach(this::closeAndLog);
    }

    private void closeAndLog(Tracer tracer) {
        try {
            tracer.close();
        } catch (Exception e) {
            String errorMessage = format("Exception when closing tracer %s.", tracer);
            log().error(errorMessage, e);
        }
    }
}
