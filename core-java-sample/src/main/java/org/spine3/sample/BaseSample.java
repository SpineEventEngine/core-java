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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.eventbus.EventBus;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.OrderRepository;
import org.spine3.server.Engine;
import org.spine3.server.storage.StorageFactory;
import org.spine3.util.Users;

import java.util.List;

/**
 * @author Mikhail Mikhaylov
 */
public abstract class BaseSample {

    protected void execute() {
        // Start the engine
        Engine.start(storageFactory());

        // Register repository with the engine. This will register it in the CommandDispatcher too.
        Engine.getInstance().register(new OrderRepository());

        // Register event handlers
        EventBus.getInstance().register(new EventLogger());

        // Generate test requests
        List<CommandRequest> requests = generateRequests();

        // Process requests
        for (CommandRequest request : requests) {
            Engine.getInstance().process(request);
        }

        log().info("All the requests were handled.");
    }

    //TODO:2015-09-23:alexander.yevsyukov: Rename and extend sample data to better reflect the problem domain.
    // See Amazon screens for correct naming of domain things.

    protected List<CommandRequest> generateRequests() {
        List<CommandRequest> result = Lists.newArrayList();

        for (int i = 0; i < 10; i++) {
            OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
            UserId userId = Users.createId("user" + i);

            CommandRequest createOrder = Requests.createOrder(userId, orderId);
            CommandRequest addOrderLine = Requests.addOrderLine(userId, orderId);
            CommandRequest payForOrder = Requests.payForOrder(userId, orderId);

            result.add(createOrder);
            result.add(addOrderLine);
            result.add(payForOrder);
        }

        return result;
    }

    protected abstract StorageFactory storageFactory();

    protected abstract Logger log();

}
