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

package io.spine.server.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.core.Event;
import io.spine.logging.Logging;
import io.spine.server.BoundedContext;
import io.spine.server.event.given.AbstractReactorTestEnv;
import io.spine.server.event.given.AbstractReactorTestEnv.FoodSafetyDepartment;
import io.spine.server.event.given.AbstractReactorTestEnv.HealthInspector;
import io.spine.server.event.given.AbstractReactorTestEnv.KitchenFront;
import io.spine.server.event.given.AbstractReactorTestEnv.RestaurantPsychologicalCounselor;
import io.spine.server.event.given.AbstractReactorTestEnv.UnluckyFoodServer;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.type.given.GivenEvent;
import io.spine.test.event.Dish;
import io.spine.test.event.DishCooked;
import io.spine.test.event.DishReturnedToKitchen;
import io.spine.test.event.DishServed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.spine.server.event.given.AbstractReactorTestEnv.FaultyHealthInspector;
import static io.spine.server.event.given.AbstractReactorTestEnv.poisonousDish;
import static io.spine.server.event.given.AbstractReactorTestEnv.someDish;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.slf4j.event.Level.ERROR;

@DisplayName("Abstract event reactor should")
class AbstractEventReactorTest {

    private BoundedContext restaurantContext;
    private BoundedContext foodSafetyContext;

    private KitchenFront kitchenFront;
    private RestaurantPsychologicalCounselor counselor;
    private HealthInspector healthInspector;
    private FoodSafetyDepartment foodSafetyDepartment;

    @BeforeEach
    void setUp() {
        TransportFactory commonTransport = InMemoryTransportFactory.newInstance();
        restaurantContext = BoundedContext
                .newBuilder()
                .setName("Restaurant context")
                .setTransportFactory(commonTransport)
                .build();
        kitchenFront = new KitchenFront(restaurantContext.eventBus());
        restaurantContext.registerEventDispatcher(kitchenFront);
        counselor = new RestaurantPsychologicalCounselor(restaurantContext.eventBus());
        restaurantContext.registerEventDispatcher(counselor);

        foodSafetyContext = BoundedContext
                .newBuilder()
                .setName("Health inspector context")
                .setTransportFactory(commonTransport)
                .build();
        healthInspector = new HealthInspector(foodSafetyContext.eventBus());
        foodSafetyDepartment = new FoodSafetyDepartment(foodSafetyContext.eventBus());
        foodSafetyContext.registerEventDispatcher(healthInspector);
        foodSafetyContext.registerEventDispatcher(foodSafetyDepartment);
    }

    @DisplayName("while dealing with domestic events")
    @Nested
    class DomesticEvents {

        @Test
        @DisplayName("receive one")
        void receive() {
            Dish dishToCook = someDish();
            DishCooked cooked = DishCooked
                    .newBuilder()
                    .setDish(dishToCook)
                    .build();
            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(cooked));
            boolean dishServed = kitchenFront.dishesServed()
                                             .contains(dishToCook.getDishId());
            assertTrue(dishServed);
        }

        @Test
        @DisplayName("receive several")
        void receiveSeveral() {
            Dish dishToCook = someDish();
            DishCooked cooked = DishCooked
                    .newBuilder()
                    .setDish(dishToCook)
                    .build();
            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(cooked));
            assertFalse(kitchenFront.dishesServed()
                                    .isEmpty());

            DishReturnedToKitchen returned = DishReturnedToKitchen
                    .newBuilder()
                    .setReturnedDish(dishToCook)
                    .build();
            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(returned));
            assertTrue(kitchenFront.secondsWastedBeingSad() > 0);
        }

        @Test
        @DisplayName("log an error")
        void logError() {
            UnluckyFoodServer server = new UnluckyFoodServer(restaurantContext.eventBus());

            Queue<SubstituteLoggingEvent> loggedMessages =
                    redirectLogging((SubstituteLogger) server.log());

            restaurantContext.registerEventDispatcher(server);
            DishCooked cooked = DishCooked
                    .newBuilder()
                    .setDish(someDish())
                    .build();
            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(cooked));

            assertLoggedCorrectly(loggedMessages);
        }

        @Test
        @DisplayName("react on one")
        void react() {
            Dish returnedDish = someDish();
            DishReturnedToKitchen dishReturnedToKitchen = DishReturnedToKitchen
                    .newBuilder()
                    .setReturnedDish(returnedDish)
                    .build();

            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(dishReturnedToKitchen));
            assertEquals(1, counselor.appointmentsScheduled());
        }

        @Test
        @DisplayName("react on several")
        void reactOnSeveral() {
            ImmutableList<Event> dishesReturned =
                    ImmutableSet.of(someDish(), someDish(), someDish())
                                .stream()
                                .map(AbstractReactorTestEnv::returnDish)
                                .map(GivenEvent::withMessage)
                                .collect(ImmutableList.toImmutableList());
            restaurantContext.eventBus()
                             .post(dishesReturned);

            assertEquals(dishesReturned.size(), counselor.appointmentsScheduled());
        }
    }

    @DisplayName("while dealing with external events")
    @Nested
    class ExternalEvents {

        @DisplayName("receive one")
        @Test
        void receive() {
            Dish poisonousDish = poisonousDish();
            DishServed served = DishServed
                    .newBuilder()
                    .setDish(poisonousDish)
                    .build();

            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(served));
            boolean poisonousDishFound = healthInspector.dishesFoundPoisonous()
                                                        .contains(poisonousDish.getDishId());
            assertTrue(poisonousDishFound);
        }

        @DisplayName("react to one")
        @Test
        void react() {
            Dish poisonousDish = poisonousDish();
            DishServed served = DishServed
                    .newBuilder()
                    .setDish(poisonousDish)
                    .build();
            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(served));
            boolean warningIssued = foodSafetyDepartment.poisonousDishes()
                                                        .contains(poisonousDish);
            assertTrue(warningIssued);
        }

        @DisplayName("log an error")
        @Test
        void logAnError() {
            FaultyHealthInspector healthInspector
                    = new FaultyHealthInspector(foodSafetyContext.eventBus());
            foodSafetyContext.registerEventDispatcher(healthInspector);

            Queue<SubstituteLoggingEvent> loggedMessages = redirectLogging(
                    (SubstituteLogger) healthInspector.log());
            Dish dishToServe = someDish();
            DishServed dishServed = DishServed
                    .newBuilder()
                    .setDish(dishToServe)
                    .build();
            restaurantContext.eventBus()
                             .post(GivenEvent.withMessage(dishServed));
            assertLoggedCorrectly(loggedMessages);
        }
    }

    /** Redirects the specified logging to a new queue, then returns the queue. */
    private static Queue<SubstituteLoggingEvent> redirectLogging(SubstituteLogger logger) {
        Queue<SubstituteLoggingEvent> result = new ArrayDeque<>();
        Logging.redirect(logger, result);
        return result;
    }

    /**
     * Makes sure that the error has been correctly logged.
     *
     * <p>Checks that:
     * <ul>
     * <li>only 1 message has been logged;
     * <li>logged message is of the {@code ERROR} level.
     * </ul>
     */
    private static void assertLoggedCorrectly(Queue<SubstituteLoggingEvent> messages) {
        assertEquals(1, messages.size());
        SubstituteLoggingEvent loggedWarning = messages.poll();
        assertEquals(ERROR, loggedWarning.getLevel());
    }
}
