/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample.server;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventContext;
import org.spine3.sample.order.event.OrderCreated;
import org.spine3.sample.order.event.OrderLineAdded;
import org.spine3.sample.order.event.OrderPayed;
import org.spine3.util.Messages;

/**
 * Sample Spine event subscriber implementation.
 *
 * @author Mikhail Melnik
 */
public class SampleSubscriber {

    public static final String NEW_LINE = System.lineSeparator();

    @Subscribe
    @SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened"})
    public void on(OrderCreated event, EventContext context) {
        log().info("Order has been created! " + NEW_LINE
                + Messages.toText(event.getOrderId()));
    }

    @Subscribe
    @SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened"})
    public void on(OrderLineAdded event, EventContext context) {
        log().info("Order line was added! " + NEW_LINE
                + Messages.toText(event.getOrderId())
                + Messages.toText(event.getOrderLine()));
    }

    @Subscribe
    @SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened"})
    public void on(OrderPayed event, EventContext context) {
        log().info("Order was payed! It is waiting to be shipped now. " + NEW_LINE
                + Messages.toText(event.getOrderId())
                + Messages.toText(event.getBillingInfo()));
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SampleSubscriber.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
