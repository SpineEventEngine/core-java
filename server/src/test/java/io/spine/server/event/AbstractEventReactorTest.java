/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.event;

import com.google.common.collect.ImmutableList;
import io.spine.server.BoundedContext;
import io.spine.server.event.given.AbstractReactorTestEnv.AutoCharityDonor;
import io.spine.server.event.given.AbstractReactorTestEnv.RestaurantNotifier;
import io.spine.server.event.given.AbstractReactorTestEnv.ServicePerformanceTracker;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.blackbox.BlackBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderPaidFor;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderReady;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderServedInTime;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderServedLate;

@DisplayName("Abstract event reactor should")
class AbstractEventReactorTest {

    private BlackBox restaurantContext;
    private BlackBox charityContext;

    private AutoCharityDonor charityDonor;
    private ServicePerformanceTracker performanceTracker;

    @BeforeEach
    void setUp() {
        performanceTracker = new ServicePerformanceTracker();
        RestaurantNotifier notifier = new RestaurantNotifier();
        restaurantContext = BlackBox.from(
                BoundedContext.singleTenant("Restaurant")
                              .addEventDispatcher(performanceTracker)
                              .addEventDispatcher(notifier)
        );

        charityDonor = new AutoCharityDonor();
        charityContext = BlackBox.from(
                BoundedContext.singleTenant("Charity")
                              .addEventDispatcher(charityDonor)
        );
    }

    @DisplayName("while dealing with domestic events")
    @Nested
    class DomesticEvents {

        @Test
        @DisplayName("receive one")
        void receive() {
            OrderServed orderServed = someOrderServedInTime();
            restaurantContext.receivesEvent(orderServed);
            ImmutableList<OrderServed> ordersServed = performanceTracker.ordersServed();
            ImmutableList<OrderServedLate> ordersServedLate = performanceTracker.ordersServedLate();
            assertThat(ordersServed).containsExactly(orderServed);
            assertThat(ordersServedLate).isEmpty();
        }

        @Test
        @DisplayName("receive several")
        void receiveSeveral() {
            ImmutableList<OrderServed> eventsToEmit = ImmutableList.of(
                    someOrderServedInTime(), someOrderServedInTime(), someOrderServedLate()
            );
            eventsToEmit.forEach(restaurantContext::receivesEvent);
            ImmutableList<OrderServed> ordersServed = performanceTracker.ordersServed();
            ImmutableList<OrderServedLate> ordersServedLate = performanceTracker.ordersServedLate();
            assertThat(ordersServed).hasSize(eventsToEmit.size());
            long ordersInTime = ordersServed
                    .stream()
                    .filter(orderServed -> !performanceTracker.servedLate(orderServed))
                    .count();
            assertThat(ordersInTime).isEqualTo(2);
            assertThat(ordersServedLate).hasSize(1);
        }

        @Test
        @DisplayName("react with one")
        void react() {
            OrderServed servedLate = someOrderServedLate();
            restaurantContext.receivesEvent(servedLate)
                             .assertEvents()
                             .withType(OrderServedLate.class)
                             .hasSize(1);
        }

        @Test
        @DisplayName("react with none")
        void reactWithNone() {
            OrderServed orderServed = someOrderServedInTime();
            restaurantContext.receivesEvent(orderServed)
                             .assertEvents()
                             .withType(OrderServedLate.class)
                             .isEmpty();
        }

        @Test
        @DisplayName("react with several events")
        void reactWithSeveral() {
            OrderReadyToBeServed orderIsReady = someOrderReady();
            EventSubject assertEvents = restaurantContext
                    .receivesEvent(orderIsReady)
                    .assertEvents();
            assertEvents.withType(CustomerNotified.class).hasSize(1);
            assertEvents.withType(DeliveryServiceNotified.class).hasSize(1);
        }
    }

    @DisplayName("while dealing with external events")
    @Nested
    class ExternalEvents {

        @DisplayName("receive one")
        @Test
        void receive() {
            OrderPaidFor orderPaidFor = someOrderPaidFor();
            charityContext.receivesExternalEvent(orderPaidFor);

            double orderCost = orderPaidFor.getOrder()
                                           .getPriceInUsd();
            double expectedDonationAmount = orderCost * 0.02;
            assertThat(charityDonor.totalDonated()).isEqualTo(expectedDonationAmount);
        }

        @DisplayName("react to one")
        @Test
        void reactToOne() {
            OrderPaidFor orderPaidFor = someOrderPaidFor();
            charityContext.receivesExternalEvent(orderPaidFor)
                          .assertEvents()
                          .withType(DonationMade.class)
                          .hasSize(1);
        }

        @DisplayName("react to several")
        @Test
        void reactToSeveral() {
            OrderPaidFor paidInRestaurant = someOrderPaidFor();
            OrderPaidFor paidToDelivery = someOrderPaidFor();

            charityContext.receivesExternalEvent(paidToDelivery)
                          .receivesExternalEvent(paidInRestaurant)
                          .assertEvents()
                          .withType(DonationMade.class)
                          .hasSize(2);
        }
    }
}
