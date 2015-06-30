/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
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
