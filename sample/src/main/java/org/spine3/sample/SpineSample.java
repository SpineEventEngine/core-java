/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.Engine;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.server.SpineSampleServer;
import org.spine3.util.UserIds;

import java.util.List;

/**
 * Simple Spine framework example.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class SpineSample {

    public static void main(String[] args) {
        SpineSampleServer.registerEventSubscribers();

        SpineSampleServer.prepareEngine();

        List<CommandRequest> requests = prepareRequests();
        for (CommandRequest request : requests) {
            Engine.getInstance().handle(request);
        }

        log().info("All the requests were handled.");
    }

    private static List<CommandRequest> prepareRequests() {
        List<CommandRequest> result = Lists.newArrayList();

        for (int i = 0; i < 10; i++) {
            OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
            UserId userId = UserIds.create("user" + i);

            CommandRequest createOrder = Requests.createOrder(userId, orderId);
            CommandRequest addOrderLine = Requests.addOrderLine(userId, orderId);
            CommandRequest payOrder = Requests.payOrder(userId, orderId);

            result.add(createOrder);
            result.add(addOrderLine);
            result.add(payOrder);
        }

        return result;
    }

    private SpineSample() {
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SpineSample.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
