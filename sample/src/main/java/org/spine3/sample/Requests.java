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

import org.spine3.base.*;
import org.spine3.sample.order.BillingInfo;
import org.spine3.sample.order.Book;
import org.spine3.sample.order.BookId;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.command.AddOrderLine;
import org.spine3.sample.order.command.CreateOrder;
import org.spine3.sample.order.command.PayOrder;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.ZoneOffset;

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
