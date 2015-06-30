/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample.order;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.AggregateRoot;
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
@SuppressWarnings("TypeMayBeWeakened")  // Use command and event classes passed as parameters instead of SomethingOrBuilder
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

    @SuppressWarnings({"unused", "MethodMayBeStatic"}) // the method is invoked by annotation.
    @Subscribe
    private Message handle(CreateOrder cmd, CommandContext ctx) {
        OrderCreated result = generateEvent(cmd);

        return result;
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"}) // the method is invoked by annotation.
    @Subscribe
    private Message handle(AddOrderLine cmd, CommandContext ctx) {
        validateCommand(cmd);

        OrderLineAdded result = generateEvent(cmd);

        return result;
    }

    //TODO:2015-06-29:alexander.yevsyukov: Consider renaming PayOrder command.

    @SuppressWarnings({"unused", "MethodMayBeStatic"}) // the method is invoked by annotation.
    @Subscribe
    private Message handle(PayOrder cmd, CommandContext ctx) {
        validateCommand(cmd);

        OrderPayed result = generateEvent(cmd);

        return result;
    }

    @SuppressWarnings("unused") // the method is invoked by annotation.
    @Subscribe
    private void on(OrderCreated event) {
        Order newState = prepareState(event);

        validate(newState);

        setState(newState);
    }

    @SuppressWarnings("unused") // the method is invoked by annotation.
    @Subscribe
    private void on(OrderLineAdded event) {
        Order newState = prepareState(event);

        validate(newState);

        setState(newState);
    }

    @SuppressWarnings("unused") // the method is invoked by annotation.
    @Subscribe
    private void on(OrderPayed event) {
        Order newState = prepareState(event);

        validate(newState);

        setState(newState);
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
