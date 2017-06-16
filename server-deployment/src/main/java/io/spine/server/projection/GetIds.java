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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.Event;
import io.spine.envelope.EventEnvelope;
import io.spine.server.entity.idfunc.EventTargetsFunction;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;

import java.util.Set;

import static com.google.common.collect.ImmutableList.builder;

/**
 * Obtains projection IDs for an event via supplied function.
 *
 * @author Alexander Yevsyukov
 */
class GetIds<I> extends SimpleFunction<Event, Iterable<KV<I, Event>>> {

    private static final long serialVersionUID = 0L;
    private final EventTargetsFunction<I, Message> fn;

    GetIds(EventTargetsFunction<I, Message> fn) {
        super();
        this.fn = fn;
    }

    @Override
    public Iterable<KV<I, Event>> apply(Event input) {
        final EventEnvelope event = EventEnvelope.of(input);
        final Set<I> idSet = fn.apply(event.getMessage(), event.getEventContext());
        final ImmutableList.Builder<KV<I, Event>> builder = builder();
        for (I id : idSet) {
            builder.add(KV.of(id, input));
        }
        return builder.build();
    }
}
