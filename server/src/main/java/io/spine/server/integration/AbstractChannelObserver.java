/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.annotation.SPI;
import io.spine.core.BoundedContextName;
import io.spine.logging.Logging;
import io.spine.server.transport.Subscriber;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Base routines for the {@linkplain Subscriber#addObserver(StreamObserver)}
 * subscriber observers}.
 */
@SPI
public abstract class AbstractChannelObserver implements StreamObserver<ExternalMessage>, Logging {

    private final BoundedContextName boundedContextName;
    private final Class<? extends Message> messageClass;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    protected AbstractChannelObserver(BoundedContextName boundedContextName,
                                      Class<? extends Message> messageClass) {
        this.boundedContextName = boundedContextName;
        this.messageClass = messageClass;
    }

    /**
     * Handles the {@linkplain ExternalMessage message} received via this channel.
     *
     * <p>This behaviour is specific to the particular channel observer implementation.
     *
     * @param message
     *         the received message
     */
    protected abstract void handle(ExternalMessage message);

    @Override
    public void onError(Throwable t) {
        boolean wasCompleted = !completed.compareAndSet(false, true);
        if (wasCompleted) {
            _warn().log("Observer for `%s` received an error despite being closed.",
                        messageClass.getName());
        }
        throw newIllegalStateException("Error caught when observing the incoming " +
                                               "messages of type %s", messageClass);
    }

    @Override
    public void onCompleted() {
        boolean wasNotCompleted = completed.compareAndSet(false, true);
        checkState(wasNotCompleted, "Observer of `%s` is already closed.", messageClass.getName());
    }

    @Override
    public final void onNext(ExternalMessage message) {
        checkNotNull(message);
        checkState(!completed.get(),
                   "Channel %s received message (%s[%s]) despite being closed.",
                   message.getClass().getName(),
                   message.getOriginalMessage().getTypeUrl());
        BoundedContextName source = message.getBoundedContextName();
        boolean sameContext = boundedContextName.equals(source)
                           || boundedContextName.isSystemOf(source)
                           || source.isSystemOf(boundedContextName);
        if (!sameContext) {
            handle(message);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractChannelObserver)) {
            return false;
        }
        AbstractChannelObserver that = (AbstractChannelObserver) o;
        return Objects.equals(boundedContextName, that.boundedContextName) &&
                Objects.equals(messageClass, that.messageClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, messageClass);
    }
}
