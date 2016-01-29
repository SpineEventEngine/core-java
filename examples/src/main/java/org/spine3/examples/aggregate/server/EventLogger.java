/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.examples.aggregate.server;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventContext;
import org.spine3.examples.aggregate.event.OrderCreated;
import org.spine3.examples.aggregate.event.OrderLineAdded;
import org.spine3.examples.aggregate.event.OrderPaid;
import org.spine3.protobuf.Messages;
import org.spine3.server.EventHandler;
import org.spine3.server.Identifiers;
import org.spine3.server.Subscribe;

/**
 * A sample of event handler class that logs some of the events.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened", "UnusedParameters"})
class EventLogger implements EventHandler {

    private static final String NEW_LINE = System.lineSeparator();

    private static String orderIdLine(Message id) {
        return "Order ID: " + Identifiers.idToString(id) + NEW_LINE;
    }

    @Subscribe
    public void on(OrderCreated event, EventContext context) {
        log().info("Order has been created. " + NEW_LINE
                + orderIdLine(event.getOrderId()));
    }

    @Subscribe
    public void on(OrderLineAdded event, EventContext context) {
        log().info("Order line was added. " + NEW_LINE
                + orderIdLine(event.getOrderId())
                + Messages.toText(event.getOrderLine()));
    }

    @Subscribe
    public void on(OrderPaid event, EventContext context) {
        log().info("Order was paid." + NEW_LINE
                + orderIdLine(event.getOrderId())
                + Messages.toText(event.getBillingInfo()));
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventLogger.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
