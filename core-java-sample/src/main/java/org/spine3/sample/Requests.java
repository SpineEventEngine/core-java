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

import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;
import org.spine3.sample.order.*;
import org.spine3.sample.order.command.AddOrderLine;
import org.spine3.sample.order.command.CreateOrder;
import org.spine3.sample.order.command.PayForOrder;
import org.spine3.sample.processmanager.command.ProcessManagerSampleCommand;
import org.spine3.time.ZoneOffset;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * Utility class for generating sample command requests.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
class Requests {

    public static CommandRequest processManagerSampleCommand(UserId id, OrderId orderId) {
        final ProcessManagerSampleCommand command = ProcessManagerSampleCommand.newBuilder().setOrderId(orderId).build();
        final CommandRequest result = newCommandRequest(id, command);
        return result;
    }

    public static CommandRequest createOrder(UserId userId, OrderId orderId) {
        final CreateOrder command = CreateOrder.newBuilder()
                .setOrderId(orderId)
                .build();
        return newCommandRequest(userId, command);
    }

    public static CommandRequest addOrderLine(UserId userId, OrderId orderId) {
        final double price = 51.33;
        final Book book = Book.newBuilder()
                .setBookId(BookId.newBuilder().setISBN("978-0321125217").build())
                .setAuthor("Eric Evans")
                .setTitle("Domain Driven Design.")
                .setPrice(price)
                .build();
        final int quantity = 1;
        final double totalPrice = book.getPrice() * quantity;
        final OrderLine orderLine = OrderLine.newBuilder()
                .setProductId(Messages.toAny(book.getBookId()))
                .setQuantity(quantity)
                .setTotal(totalPrice)
                .build();
        final AddOrderLine command = AddOrderLine.newBuilder()
                .setOrderId(orderId)
                .setOrderLine(orderLine).build();
        return newCommandRequest(userId, command);
    }

    public static CommandRequest payForOrder(UserId userId, OrderId orderId) {
        final BillingInfo billingInfo = BillingInfo.newBuilder().setInfo("Payment info is here.").build();
        final PayForOrder command = PayForOrder.newBuilder()
                .setOrderId(orderId)
                .setBillingInfo(billingInfo)
                .build();
        return newCommandRequest(userId, command);
    }

    public static CommandRequest newCommandRequest(UserId userId, Message command) {
        final CommandContext context = getCommandContext(userId);
        final CommandRequest.Builder request = CommandRequest.newBuilder()
                .setCommand(Messages.toAny(command))
                .setContext(context);
        return request.build();
    }

    public static CommandContext getCommandContext(UserId userId) {
        final CommandId commandId = CommandId.newBuilder()
                .setActor(userId)
                .setTimestamp(getCurrentTime())
                .build();
        return CommandContext.newBuilder()
                .setCommandId(commandId)
                .setZoneOffset(ZoneOffset.getDefaultInstance())
                .build();
    }

    private Requests() {
    }
}
