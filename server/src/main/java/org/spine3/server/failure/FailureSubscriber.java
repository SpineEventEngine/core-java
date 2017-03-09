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
package org.spine3.server.failure;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.server.reflect.FailureSubscriberMethod;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * The abstract base for objects that can be subscribed to receive business failures
 * from {@link org.spine3.server.failure.FailureBus FailureBus}.
 *
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 * @see org.spine3.server.failure.FailureBus#register(org.spine3.server.bus.MessageDispatcher)
 */
public class FailureSubscriber implements FailureDispatcher {
    /**
     * Cached set of the event classes this subscriber is subscribed to.
     */
    @Nullable
    private Set<FailureClass> failureClasses;

    @Override
    public void dispatch(FailureEnvelope envelope) {
        handle(envelope.getMessage(), envelope.getCommandMessage(), envelope.getCommandContext());
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<FailureClass> getMessageClasses() {
        if (failureClasses == null) {
            failureClasses = ImmutableSet.copyOf(
                    FailureSubscriberMethod.getFailureClasses(getClass()));
        }
        return failureClasses;
    }

    public void handle(Message failureMessage, Message commandMessage, CommandContext context) {
        FailureSubscriberMethod.invokeSubscriber(this,
                                                 failureMessage, commandMessage, context);
    }
}
