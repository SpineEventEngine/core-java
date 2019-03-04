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
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.EventBus;
import io.spine.server.event.React;
import io.spine.test.event.AppointmentMade;
import io.spine.test.event.Dish;
import io.spine.test.event.DishCooked;
import io.spine.test.event.DishFoundToBePoisonous;
import io.spine.test.event.DishReturnedToKitchen;
import io.spine.test.event.DishServed;
import io.spine.test.event.HeadChefGotSad;
import io.spine.test.event.RestaurantWarningMade;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.spine.protobuf.Timestamps2.fromInstant;
import static java.lang.String.format;
import static java.time.Instant.now;

public class AbstractReactorTestEnv {

    /** Prevent instantiation. */
    private AbstractReactorTestEnv() {
    }

    /** Returns a dish with a random ID. */
    public static Dish someDish() {
        String dishId = Identifier.newUuid();
        String dishName = format("Name of dish `%s`.", dishId);

        Dish result = Dish
                .newBuilder()
                .setDishId(dishId)
                .setDishName(dishName)
                .build();
        return result;
    }

    /** Obtains an event that signifies that the specified dish got returned to the kitchen. */
    public static DishReturnedToKitchen returnDish(Dish dishToReturn) {
        DishReturnedToKitchen result = DishReturnedToKitchen
                .newBuilder()
                .setReturnedDish(dishToReturn)
                .build();
        return result;
    }

    /** Obtains a dish that is considered poisonous by the {@link HealthInspector}. */
    public static Dish poisonousDish() {
        String dishId = Identifier.newUuid();
        String dishName = format("Gluten-containing dish `%s`.", dishId);
        Dish result = Dish
                .newBuilder()
                .setDishId(dishId)
                .setDishName(dishName)
                .build();
        return result;
    }

    /**
     * Oversees mental health of the restaurant workers by appointing a counseling session
     * once any of them get sad.
     */
    public static class RestaurantPsychologicalCounselor extends AbstractEventReactor {

        private int appointmentsScheduled = 0;

        public RestaurantPsychologicalCounselor(EventBus eventBus) {
            super(eventBus);
        }

        @React
        AppointmentMade makeAnAppointment(HeadChefGotSad headChefGotSad) {
            Timestamp timeOfAppointment = fromInstant(now().plus(1, ChronoUnit.DAYS));
            AppointmentMade result = AppointmentMade
                    .newBuilder()
                    .setAppointmentTime(timeOfAppointment)
                    .build();
            appointmentsScheduled++;
            return result;
        }

        public int appointmentsScheduled() {
            return appointmentsScheduled;
        }
    }

    /**
     * Serves dishes as soon as they have been cooked and makes the head chef sad if the dish got
     * returned.
     *
     * <p>Does so by emitting a respective events.
     */
    public static class KitchenFront extends AbstractEventReactor {

        /** IDs of dishes served by this server. */
        private final List<String> dishesServed = new ArrayList<>();
        private static final int CHEF_COOLDOWN = 5;
        private int secondsWastedBeingSad = 0;

        public KitchenFront(EventBus eventBus) {
            super(eventBus);
        }

        @React
        DishServed serveDish(DishCooked dishCooked) {
            Dish dish = dishCooked.getDish();
            dishesServed.add(dish.getDishId());
            DishServed result = DishServed
                    .newBuilder()
                    .setDish(dish)
                    .build();
            return result;
        }

        @React
        HeadChefGotSad getSad(DishReturnedToKitchen dishReturned) {
            HeadChefGotSad result = HeadChefGotSad
                    .newBuilder()
                    .setSadnessDuration(CHEF_COOLDOWN)
                    .build();
            secondsWastedBeingSad += CHEF_COOLDOWN;
            return result;
        }

        public ImmutableList<String> dishesServed() {
            return ImmutableList.copyOf(dishesServed);
        }

        public int secondsWastedBeingSad() {
            return secondsWastedBeingSad;
        }
    }

    /**
     * Labels dishes as poisonous as soon as they get served by emitting a respective event.
     */
    public static class HealthInspector extends AbstractEventReactor {

        /** IDs of dishes that have been found to be poisonous. */
        private final List<String> dishesFoundPoisonous = new ArrayList<>();

        public HealthInspector(EventBus eventBus) {
            super(eventBus);
        }

        @React(external = true)
        Optional<DishFoundToBePoisonous> inspectDish(DishServed dishServed) {
            if (isPoisonous(dishServed)) {
                String id = dishServed.getDish()
                                      .getDishId();
                dishesFoundPoisonous.add(id);
                return Optional.of(poisonousDish(dishServed));
            } else {
                return Optional.empty();
            }
        }

        /** Returns a list of all dishes that have been found to be poisonous. */
        public ImmutableList<String> dishesFoundPoisonous() {
            return ImmutableList.copyOf(dishesFoundPoisonous);
        }

        private static DishFoundToBePoisonous poisonousDish(DishServed served) {
            DishFoundToBePoisonous result = DishFoundToBePoisonous
                    .newBuilder()
                    .setDish(served.getDish())
                    .build();
            return result;
        }

        private static boolean isPoisonous(DishServed served) {
            String name = served.getDish()
                                .getDishName();
            // Very suspicious.
            return name.toLowerCase()
                       .contains("gluten");
        }
    }

    /**
     * Issues warnings to the restaurant if it has been found to serve poisonous food.
     */
    public static class FoodSafetyDepartment extends AbstractEventReactor {

        private int warningsIssued = 0;

        public FoodSafetyDepartment(EventBus eventBus) {
            super(eventBus);
        }

        @React
        RestaurantWarningMade makeWarning(DishFoundToBePoisonous foundToBePoisonous) {
            String warning =
                    format("The restaurant has been found to serve poisonous food. " +
                                   "This is the warning number %s.", warningsIssued);
            RestaurantWarningMade result = RestaurantWarningMade
                    .newBuilder()
                    .setWarningText(warning)
                    .build();
            warningsIssued++;
            return result;
        }

        public int warningsIssued(){
            return warningsIssued;
        }
    }
}
