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
package io.spine.server.integration;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionContext;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.protobuf.AnyPacker;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.rejection.RejectionDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An adapter for {@link RejectionBus} to use it along with {@link IntegrationBus}.
 *
 * @author Alex Tymchenko
 */
final class RejectionBusAdapter extends BusAdapter<RejectionEnvelope, RejectionDispatcher<?>> {

    RejectionBusAdapter(Builder builder) {
        super(builder);
    }

    static Builder builderWith(RejectionBus rejectionBus, BoundedContextName boundedContextName) {
        checkNotNull(rejectionBus);
        checkNotNull(boundedContextName);
        return new Builder(rejectionBus, boundedContextName);
    }

    @Override
    ExternalMessageEnvelope toExternalEnvelope(ExternalMessage message) {
        Any packedRejection = message.getOriginalMessage();
        Rejection rejection = AnyPacker.unpack(packedRejection);
        ExternalMessageEnvelope result =
                ExternalMessageEnvelope.of(message, Rejections.getMessage(rejection));
        return result;
    }

    @Override
    ExternalMessageEnvelope markExternal(ExternalMessage externalMsg) {
        Any packedEvent = externalMsg.getOriginalMessage();
        Rejection rejection = AnyPacker.unpack(packedEvent);
        Rejection.Builder rejectionBuilder = rejection.toBuilder();
        RejectionContext modifiedContext = rejectionBuilder.getContext()
                                                           .toBuilder()
                                                           .setExternal(true)
                                                           .build();

        Rejection marked = rejectionBuilder.setContext(modifiedContext)
                                           .build();
        ExternalMessage result = ExternalMessages.of(marked,
                                                     externalMsg.getBoundedContextName());
        return ExternalMessageEnvelope.of(result, Rejections.getMessage(rejection));
    }

    @Override
    boolean accepts(Class<? extends Message> messageClass) {
        return Rejection.class == messageClass;
    }

    @Override
    RejectionDispatcher<?> createDispatcher(Class<? extends Message> messageClass) {
        DomesticRejectionPublisher result =
                new DomesticRejectionPublisher(getBoundedContextName(),
                                               getPublisherHub(),
                                               RejectionClass.of(messageClass));
        return result;
    }

    static class Builder extends AbstractBuilder<Builder,
            RejectionEnvelope,
            RejectionDispatcher<?>> {

        Builder(RejectionBus eventBus, BoundedContextName boundedContextName) {
            super(eventBus, boundedContextName);
        }

        @Override
        protected RejectionBusAdapter doBuild() {
            return new RejectionBusAdapter(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
