/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample;

import org.spine3.base.*;
import org.spine3.sample.order.BillingInfo;
import org.spine3.sample.order.Book;
import org.spine3.sample.order.BookId;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.command.AddOrderLine;
import org.spine3.sample.order.command.CreateOrder;
import org.spine3.sample.order.command.PayOrder;
import org.spine3.util.Messages;
import org.spine3.util.Timestamps;

/**
 * Utility class for generating sample command requests.
 * For test usage only.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class Requests {

    public static CommandRequest createOrder(UserId userId, OrderId orderId) {
        CreateOrder createOrder = CreateOrder.newBuilder()
                .setOrderId(orderId)
                .build();

        CommandContext context = getCommandContext(userId);

        CommandRequest result = CommandRequest.newBuilder()
                .setCommand(Messages.toAny(createOrder))
                .setContext(context)
                .build();

        return result;
    }

    public static CommandRequest addOrderLine(UserId userId, OrderId orderId) {
        //noinspection MagicNumber
        Book book = Book.newBuilder()
                .setBookId(BookId.newBuilder().setISBN("978-0321125217").build())
                .setAuthor("Eric Ewans")
                .setTitle("Domain Driven Design.")
                .setPrice(51.33)
                .build();

        int quantity = 1;
        double price = book.getPrice() * quantity;

        org.spine3.sample.order.OrderLine orderLine = org.spine3.sample.order.OrderLine.newBuilder()
                .setProductId(Messages.toAny(book.getBookId()))
                .setQuantity(quantity)
                .setTotal(price)
                .build();

        AddOrderLine cmd = AddOrderLine.newBuilder()
                .setOrderId(orderId)
                .setOrderLine(orderLine).build();

        CommandRequest result = CommandRequest.newBuilder()
                .setCommand(Messages.toAny(cmd))
                .setContext(getCommandContext(userId)).build();
        return result;
    }

    public static CommandRequest payOrder(UserId userId, OrderId orderId) {
        BillingInfo billingInfo = BillingInfo.newBuilder().setInfo("Paying info is here.").build();

        PayOrder cmd = PayOrder.newBuilder()
                .setOrderId(orderId)
                .setBillingInfo(billingInfo)
                .build();

        CommandRequest result = CommandRequest.newBuilder()
                .setCommand(Messages.toAny(cmd))
                .setContext(getCommandContext(userId))
                .build();
        return result;
    }

    public static CommandContext getCommandContext(UserId userId) {
        CommandId commandId = CommandId.newBuilder()
                .setActor(userId)
                .setTimestamp(Timestamps.now())
                .build();
        return CommandContext.newBuilder()
                .setCommandId(commandId)
                .setZoneOffset(ZoneOffset.getDefaultInstance())
                .build();
    }

    private Requests() {
    }

}
