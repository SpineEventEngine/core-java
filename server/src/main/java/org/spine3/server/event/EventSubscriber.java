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

package org.spine3.server.event;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.server.reflect.EventSubscriberMethod;

import java.lang.reflect.InvocationTargetException;

import static org.spine3.server.reflect.EventSubscriberMethod.forMessage;

/**
 * The abstract base for objects that can be subscribed to receive events from {@link EventBus}.
 *
 * <p>Objects may also receive events via {@link EventDispatcher}s that can be
 * registered with {@code EventBus}.
 *
 * @author Alexander Yevsyukov
 * @see EventBus#subscribe(EventSubscriber)
 * @see EventBus#register(EventDispatcher)
 */
public abstract class EventSubscriber {

    public void handle(Message eventMessage, EventContext context)
            throws InvocationTargetException {
        final EventSubscriberMethod method = forMessage(getClass(), eventMessage);
        method.invoke(this, eventMessage, context);
    }
}
