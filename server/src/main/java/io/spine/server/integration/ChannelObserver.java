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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Base routines for the {@linkplain TransportFactory.MessageChannel message channel} observers.
 */
abstract class ChannelObserver implements StreamObserver<ExternalMessage> {

    private final BoundedContextId boundedContextId;
    private final Class<? extends Message> messageClass;

    protected ChannelObserver(BoundedContextId boundedContextId,
                              Class<? extends Message> messageClass) {
        this.boundedContextId = boundedContextId;
        this.messageClass = messageClass;
    }

    @Override
    public void onError(Throwable t) {
        throw newIllegalStateException("Error caught when observing the incoming " +
                                               "messages of type %s", messageClass);
    }

    @Override
    public void onCompleted() {
        throw newIllegalStateException("Unexpected 'onCompleted' when observing " +
                                               "the incoming messages of type %s",
                                       messageClass);
    }

    @Override
    public final void onNext(ExternalMessage message) {
        checkNotNull(message);

        final BoundedContextId source = message.getBoundedContextId();
        if (this.boundedContextId.equals(source)){
            return;
        }
        handle(message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChannelObserver that = (ChannelObserver) o;
        return Objects.equals(boundedContextId, that.boundedContextId) &&
                Objects.equals(messageClass, that.messageClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextId, messageClass);
    }

    protected abstract void handle(ExternalMessage message);
}
