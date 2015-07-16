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
package org.spine3.sample;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
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
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class SpineSample {

    private static final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    public static void main(String[] args) {
        helper.setUp();

        SpineSampleServer.registerEventSubscribers();

        SpineSampleServer.prepareEngine();

        List<CommandRequest> requests = prepareRequests();
        for (CommandRequest request : requests) {
            Engine.getInstance().handle(request);
        }

        log().info("All the requests were handled.");
        helper.tearDown();
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
