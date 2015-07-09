/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.lang;

import com.google.protobuf.Message;
import org.spine3.engine.MessageSubscriber;

/**
 * Exception that is thrown when more than one applier
 * of the same event type were found in the event dispatcher instance.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class EventApplierAlreadyRegisteredException extends RuntimeException {

    /**
     * Accepts event type and both old and new handlers.
     *
     * @param eventType            type of the event
     * @param currentSubscriber    a method currently registered for the given message type
     * @param discoveredSubscriber a new subscriber for the event type
     */
    public EventApplierAlreadyRegisteredException(
            Class<? extends Message> eventType,
            MessageSubscriber currentSubscriber,
            MessageSubscriber discoveredSubscriber) {

        super("The event " + eventType + " already has associated applier method" + currentSubscriber.getFullName() + '.'
                + " There can be only one applier per event type. "
                + " You attempt to register applier " + discoveredSubscriber.getFullName() + '.'
                + " If this is an intended operation, consider un-registering the current applier first.");
    }

    private static final long serialVersionUID = 0L;
}
