/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.protobuf.Durations2;
import io.spine.protobuf.Timestamps2;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.CustomerNotified;
import io.spine.server.event.CustomerNotified.NotificationMethod;
import io.spine.server.event.DeliveryServiceNotified;
import io.spine.server.event.DonationMade;
import io.spine.server.event.EventBus;
import io.spine.server.event.OrderPaidFor;
import io.spine.server.event.OrderReadyToBeServed;
import io.spine.server.event.OrderServed;
import io.spine.server.event.OrderServedLate;
import io.spine.server.event.React;
import io.spine.server.tuple.Pair;
import io.spine.test.event.Order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.Durations2.isGreaterThan;
import static io.spine.server.event.CustomerNotified.NotificationMethod.SMS;
import static java.lang.String.format;

/** Environment for abstract event reactor testing. */
public class AbstractReactorTestEnv {

    /** Prevent instantiation. */
    private AbstractReactorTestEnv() {
    }

    /** Obtains an event that signifies that some order was served in time. */
    public static OrderServed someOrderServedInTime() {
        Timestamp now = Timestamps2.fromInstant(Instant.now());
        return someOrderServedOn(now);
    }

    /** Obtains an event that signifies that some order was sered late. */
    public static OrderServed someOrderServedLate() {
        Timestamp now = Timestamps2.fromInstant(Instant.now());
        Timestamp twoHoursAfter = Timestamps.add(now, Durations2.fromHours(2));
        return someOrderServedOn(twoHoursAfter);
    }

    private static OrderServed someOrderServedOn(Timestamp timeServed) {
        OrderServed result = OrderServed
                .newBuilder()
                .setServedOn(timeServed)
                .setOrder(someOrder())
                .build();
        return result;
    }

    private static Order someOrder() {
        Timestamp now = Timestamps2.fromInstant(Instant.now());
        Order result = Order
                .newBuilder()
                .setOrderId(newUuid())
                .setPriceInUsd(somePrice())
                .setTimePlaced(now)
                .build();
        return result;
    }

    private static double somePrice() {
        @SuppressWarnings("UnsecureRandomNumberGeneration") /* Does not matter for this test case.*/
                Random random = new Random();
        double min = 10.0d;
        double max = 100.0d;
        double result = min + (max - min) * random.nextDouble();
        return result;
    }

    /** Obtains an event that signifies that some order got paid for. */
    public static OrderPaidFor someOrderPaidFor() {
        OrderPaidFor result = OrderPaidFor
                .newBuilder()
                .setOrder(someOrder())
                .build();
        return result;
    }

    /**
     * Makes a charity donation every time an order is payed for.
     *
     * <p>Donation amount is 2% of the order price.
     */
    public static class AutoCharityDonor extends AbstractEventReactor {

        private static final double DONATION_PERCENTAGE = 0.02d;

        /** Total USDs donated by this donor. */
        private double totalDonated = 0.0d;

        public AutoCharityDonor(EventBus eventBus) {
            super(eventBus);
        }

        @React(external = true)
        DonationMade donateToCharity(OrderPaidFor orderPaidFor) {
            Order order = orderPaidFor.getOrder();
            double orderPrice = order.getPriceInUsd();
            double donationAmount = orderPrice * DONATION_PERCENTAGE;
            totalDonated += donationAmount;
            DonationMade result = DonationMade
                    .newBuilder()
                    .setUsdsDonated(donationAmount)
                    .build();
            return result;
        }

        public double totalDonated() {
            return totalDonated;
        }
    }

    /**
     * Tracks the performance of the restaurant servers.
     *
     * <p>If an order was served late i.e. after more than 50 minutes after it was placed,
     * emits a respective event.
     */
    public static class ServicePerformanceTracker extends AbstractEventReactor {

        /** If the order is not served in 50 minutes, it is considered to be late. */
        private static final Duration BEFORE_SERVED_LATE = Durations2.fromMinutes(50);

        private final List<OrderServed> ordersServed = new ArrayList<>();
        private final List<OrderServedLate> ordersServedLate = new ArrayList<>();

        public ServicePerformanceTracker(EventBus eventBus) {
            super(eventBus);
        }

        @React
        Optional<OrderServedLate> accept(OrderServed served) {
            ordersServed.add(served);
            if (servedLate(served)) {
                Order servedOrder = served.getOrder();
                OrderServedLate result = OrderServedLate
                        .newBuilder()
                        .setOrder(servedOrder)
                        .setServedOn(now())
                        .build();
                ordersServedLate.add(result);
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Returns {@code true} if the specified {@code orderServed} event signifies a late-served
         * order, {@code false} otherwise.
         */
        public boolean servedLate(OrderServed orderServed) {
            Timestamp now = now();
            Timestamp servedOn = orderServed.getServedOn();
            Duration timeToServe = Timestamps.between(now, servedOn);
            boolean servedLate = isGreaterThan(timeToServe, BEFORE_SERVED_LATE);
            return servedLate;
        }

        /** Obtains all of the served orders. */
        public ImmutableList<OrderServed> ordersServed() {
            return ImmutableList.copyOf(ordersServed);
        }

        /** Obtains all of the orders that were served late. */
        public ImmutableList<OrderServedLate> ordersServedLate() {
            return ImmutableList.copyOf(ordersServedLate);
        }

        private static Timestamp now() {
            Instant currentInstant = Instant.now();
            Timestamp result = Timestamps2.fromInstant(currentInstant);
            return result;
        }
    }

    /**
     * Notifies customers and the delivery service when an order is ready to be served by emitting
     * a respective event.
     *
     * <p>By default, uses SMS to notify customers.
     */
    public static class RestaurantNotifier extends AbstractEventReactor {

        private final NotificationMethod notificationMethod;

        public RestaurantNotifier(EventBus eventBus) {
            super(eventBus);
            notificationMethod = SMS;
        }

        @React
        Pair<CustomerNotified, DeliveryServiceNotified>
        notifyAboutOrder(OrderReadyToBeServed orderReady) {
            Order order = orderReady.getOrder();
            CustomerNotified customerNotified = notifyCustomer(order);
            DeliveryServiceNotified deliveryNotified = notifyDelivery(order);
            Pair<CustomerNotified, DeliveryServiceNotified> result = Pair.of(customerNotified,
                                                                             deliveryNotified);
            return result;
        }

        private CustomerNotified notifyCustomer(Order order) {
            CustomerNotified result = CustomerNotified
                    .newBuilder()
                    .setOrder(order)
                    .setNotificationMethod(notificationMethod)
                    .build();
            return result;
        }

        private static DeliveryServiceNotified notifyDelivery(Order order) {
            String messageFormat = "Order %s is ready to be delivered.";
            DeliveryServiceNotified result = DeliveryServiceNotified
                    .newBuilder()
                    .setMessage(format(messageFormat, order.getOrderId()))
                    .setOrder(order)
                    .build();
            return result;
        }
    }

    /** Obtains an event that signifies that some order is ready to be served. */
    public static OrderReadyToBeServed someOrderReady() {
        OrderReadyToBeServed result = OrderReadyToBeServed
                .newBuilder()
                .setOrder(someOrder())
                .build();
        return result;
    }

    /** Throws an exception whenever an order is ready. */
    public static class FaultyNotifier extends AbstractEventReactor {

        public FaultyNotifier(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("NewExceptionWithoutArguments") /* Does not matter for this test case. */
        @React
        Pair<CustomerNotified, DeliveryServiceNotified>
        notifyAboutOrder(OrderReadyToBeServed orderReady) {
            throw new RuntimeException();
        }
    }

    /** In an attempt to donate to charity, throws an exception every time an order is payed for. */
    public static class FaultyCharityDonor extends AbstractEventReactor {

        public FaultyCharityDonor(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("NewExceptionWithoutArguments") /* Does not matter for this test case. */
        @React(external = true)
        DonationMade makeDonation(OrderPaidFor orderPaidFor) {
            throw new RuntimeException();
        }
    }
}
