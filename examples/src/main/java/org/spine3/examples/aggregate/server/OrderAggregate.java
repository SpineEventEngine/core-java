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

import org.spine3.base.CommandContext;
import org.spine3.examples.aggregate.Order;
import org.spine3.examples.aggregate.OrderId;
import org.spine3.examples.aggregate.OrderLine;
import org.spine3.examples.aggregate.command.AddOrderLine;
import org.spine3.examples.aggregate.command.CreateOrder;
import org.spine3.examples.aggregate.command.PayForOrder;
import org.spine3.examples.aggregate.event.OrderCreated;
import org.spine3.examples.aggregate.event.OrderLineAdded;
import org.spine3.examples.aggregate.event.OrderPaid;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;

/**
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("TypeMayBeWeakened" /* Use command and event classes for parameters as messages instead of SomethingOrBuilder */)
public class OrderAggregate extends Aggregate<OrderId, Order, Order.Builder> {

    public OrderAggregate(OrderId id) {
        super(id);
    }

    @Assign
    public OrderCreated handle(CreateOrder cmd, CommandContext ctx) {
        final OrderCreated result = OrderCreated.newBuilder()
                .setOrderId(cmd.getOrderId())
                .build();
        return result;
    }

    @Assign
    public OrderLineAdded handle(AddOrderLine cmd, CommandContext ctx) {
        validateCommand(cmd);

        final OrderLine orderLine = cmd.getOrderLine();
        final OrderLineAdded result = OrderLineAdded.newBuilder()
                .setOrderId(cmd.getOrderId())
                .setOrderLine(orderLine)
                .build();
        return result;
    }

    @Assign
    public OrderPaid handle(PayForOrder cmd, CommandContext ctx) {
        validateCommand(cmd);

        final OrderPaid result = OrderPaid.newBuilder()
                .setBillingInfo(cmd.getBillingInfo())
                .setOrderId(cmd.getOrderId())
                .build();
        return result;
    }

    @Apply
    private void event(OrderCreated event) {
        getBuilder()
                .setOrderId(event.getOrderId())
                .setStatus(Order.Status.NEW);
    }

    @Apply
    private void event(OrderLineAdded event) {
        final OrderLine orderLine = event.getOrderLine();
        final Order currentState = getState();
        getBuilder()
                .setOrderId(event.getOrderId())
                .addOrderLine(orderLine)
                .setTotal(currentState.getTotal() + orderLine.getTotal());
    }

    @Apply
    private void event(OrderPaid event) {
        getBuilder()
                .setBillingInfo(event.getBillingInfo())
                .setStatus(Order.Status.PAID);
    }

    private static void validateCommand(AddOrderLine cmd) {
        final OrderLine orderLine = cmd.getOrderLine();

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

    private static void validateCommand(PayForOrder cmd) {
        if (!cmd.hasOrderId()) {
            throw new IllegalArgumentException("Order ID is missing: " + cmd);
        }
    }
}
