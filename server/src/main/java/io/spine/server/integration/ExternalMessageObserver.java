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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;
import io.spine.grpc.StreamObservers;

/**
 * An observer of the incoming external messages of the specified message class.
 *
 * <p>Responsible of receiving those from the transport layer and posting those to the local
 * instance of {@code IntegrationBus}.
 */
final class ExternalMessageObserver extends AbstractChannelObserver {

    private final IntegrationBus integrationBus;

    ExternalMessageObserver(BoundedContextName boundedContextName,
                            Class<? extends Message> messageClass,
                            IntegrationBus integrationBus) {
        super(boundedContextName, messageClass);
        this.integrationBus = integrationBus;
    }

    @Override
    protected void handle(ExternalMessage message) {
        integrationBus.post(message, StreamObservers.noOpObserver());
    }
}
