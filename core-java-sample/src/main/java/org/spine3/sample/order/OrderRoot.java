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
package org.spine3.sample.order;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.server.AggregateRoot;
import org.spine3.base.CommandContext;
import org.spine3.sample.order.command.AddOrderLine;
import org.spine3.sample.order.command.CreateOrder;
import org.spine3.sample.order.command.PayOrder;
import org.spine3.sample.order.event.OrderCreated;
import org.spine3.sample.order.event.OrderLineAdded;
import org.spine3.sample.order.event.OrderPayed;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings({"TypeMayBeWeakened", "InstanceMethodNamingConvention", "MethodMayBeStatic"})
// Use command and event classes passed as parameters instead of SomethingOrBuilder
public class OrderRoot extends AggregateRoot<OrderId, Order> {

    public static final String NEW = "NEW";
    public static final String PAID = "PAID";

    public OrderRoot(OrderId id) {
        super(id);
    }

    @Override
    protected Order getDefaultState() {
        return Order.getDefaultInstance();
    }

    @Subscribe
    private Message handle(CreateOrder cmd, CommandContext ctx) {
        OrderCreated result = generateEvent(cmd);

        return result;
    }

    @Subscribe
    private Message handle(AddOrderLine cmd, CommandContext ctx) {
        validateCommand(cmd);

        OrderLineAdded result = generateEvent(cmd);

        return result;
    }

    //TODO:2015-06-29:alexander.yevsyukov: Consider renaming PayOrder command.

    @Subscribe
    private Message handle(PayOrder cmd, CommandContext ctx) {
        validateCommand(cmd);

        OrderPayed result = generateEvent(cmd);

        return result;
    }

    @Subscribe
    private void on(OrderCreated event) {
        Order newState = prepareState(event);

        validate(newState);
        incrementState(newState);
    }

    @Subscribe
    private void on(OrderLineAdded event) {
        Order newState = prepareState(event);

        validate(newState);
        incrementState(newState);
    }

    @Subscribe
    private void on(OrderPayed event) {
        Order newState = prepareState(event);

        validate(newState);
        incrementState(newState);
    }

    private static void validateCommand(AddOrderLine cmd) {
        OrderLine orderLine = cmd.getOrderLine();

        if (orderLine.getProductId() == null) {
            throw new IllegalArgumentException("Product is not set");
        }
        if (orderLine.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (orderLine.getTotal() <= 0) {
            throw new IllegalArgumentException("Total price must be positive.");
        }
    }

    private static void validateCommand(PayOrder cmd) {
        // Billing info validation is here.
    }

    private static OrderCreated generateEvent(CreateOrder cmd) {
        OrderCreated result = OrderCreated.newBuilder().setOrderId(cmd.getOrderId()).build();
        return result;
    }

    private static OrderLineAdded generateEvent(AddOrderLine cmd) {
        OrderLine orderLine = cmd.getOrderLine();
        return OrderLineAdded.newBuilder()
                .setOrderId(cmd.getOrderId())
                .setOrderLine(orderLine)
                .build();
    }

    private static OrderPayed generateEvent(PayOrder cmd) {
        OrderPayed result = OrderPayed.newBuilder()
                .setBillingInfo(cmd.getBillingInfo())
                .setOrderId(cmd.getOrderId())
                .build();
        return result;
    }

    private Order prepareState(OrderCreated event) {
        return Order.newBuilder(getState())
                .setOrderId(event.getOrderId())
                .setStatus(NEW)
                .build();
    }

    private Order prepareState(OrderLineAdded event) {
        OrderLine orderLine = event.getOrderLine();
        Order currentState = getState();
        return Order.newBuilder(currentState)
                .setOrderId(event.getOrderId())
                .addOrderLine(orderLine)
                .setTotal(currentState.getTotal() + orderLine.getTotal())
                .build();
    }

    private Order prepareState(OrderPayed event) {
        Order currentState = getState();
        Order result = Order.newBuilder(currentState)
                .setBillingInfo(event.getBillingInfo())
                .setStatus(PAID)
                .build();
        return result;
    }

}
