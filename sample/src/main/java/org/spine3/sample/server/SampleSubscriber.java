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
