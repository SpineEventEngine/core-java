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
import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.EventBus;
import io.spine.server.event.React;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.Pair;
import io.spine.test.event.AppointmentMade;
import io.spine.test.event.Dish;
import io.spine.test.event.DishCooked;
import io.spine.test.event.DishFoundToBePoisonous;
import io.spine.test.event.DishReturnedToKitchen;
import io.spine.test.event.DishServed;
import io.spine.test.event.HeadChefGotSad;
import io.spine.test.event.RestaurantPremisesCleared;
import io.spine.test.event.RestaurantShutDown;
import io.spine.test.event.RestaurantWarningMade;
import io.spine.test.event.StaffEscorted;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.Timestamps2.fromInstant;
import static java.lang.String.format;
import static java.time.Instant.now;

/** Environment for abstract event reactor testing. */
public class AbstractReactorTestEnv {

    /** Prevent instantiation. */
    private AbstractReactorTestEnv() {
    }

    /** Obtains a dish with a random ID. */
    public static Dish someDish() {
        String dishId = newUuid();
        String dishName = format("Name of dish `%s`.", dishId);

        Dish result = Dish
                .newBuilder()
                .setDishId(dishId)
                .setDishName(dishName)
                .build();
        return result;
    }

    /** Returns an event that signifies that the specified dish got returned to the kitchen. */
    public static DishReturnedToKitchen returnDish(Dish dishToReturn) {
        DishReturnedToKitchen result = DishReturnedToKitchen
                .newBuilder()
                .setReturnedDish(dishToReturn)
                .build();
        return result;
    }

    /**
     * Returns an event that signifies that the specified restaurant has served the specified dish.
     */
    public static DishServed serveDish(Dish dishToServe, String restaurantId) {
        DishServed result = DishServed
                .newBuilder()
                .setDish(dishToServe)
                .setRestaurantId(restaurantId)
                .build();
        return result;
    }

    /** Obtains a dish that is considered poisonous by the {@link HealthInspector}. */
    public static Dish poisonousDish() {
        String dishId = newUuid();
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
     *
     * <p>Emits {@code AppointmentMade} events.
     */
    public static class RestaurantPsychologicalCounselor extends AbstractEventReactor {

        private int appointmentsScheduled = 0;

        public RestaurantPsychologicalCounselor(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("unused") /* Event is ignored since the reaction result
                                       does not depend on it. */
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

        /** Obtains an amount of appointments scheduled by this counselor. */
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

        private static final int CHEF_COOLDOWN = 5;

        /** IDs of dishes served by this server. */
        private final List<String> dishesServed = new ArrayList<>();

        private int secondsWastedBeingSad = 0;

        private final String restaurantId;

        public KitchenFront(EventBus eventBus) {
            super(eventBus);
            this.restaurantId = newUuid();
        }

        @React
        DishServed serveDish(DishCooked dishCooked) {
            Dish dish = dishCooked.getDish();
            dishesServed.add(dish.getDishId());
            DishServed result = DishServed
                    .newBuilder()
                    .setDish(dish)
                    .setRestaurantId(restaurantId)
                    .build();
            return result;
        }

        @SuppressWarnings("unused") /* Event is unused since the reaction result
                                       does not depend on it. */
        @React
        HeadChefGotSad getSad(DishReturnedToKitchen dishReturned) {
            HeadChefGotSad result = HeadChefGotSad
                    .newBuilder()
                    .setSadnessDuration(CHEF_COOLDOWN)
                    .build();
            secondsWastedBeingSad += CHEF_COOLDOWN;
            return result;
        }

        /** Obtains IDs of all dishes served by this kitchen. */
        public ImmutableList<String> dishesServed() {
            return ImmutableList.copyOf(dishesServed);
        }

        /** Obtains the amount of seconds the head chef had spent sad because of returned dishes. */
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
            String restaurantId = served.getRestaurantId();
            DishFoundToBePoisonous result = DishFoundToBePoisonous
                    .newBuilder()
                    .setDish(served.getDish())
                    .setRestaurantId(restaurantId)
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
     *
     * <p>If the restaurant has served more than three poisonous dishes, shuts the restaurant down,
     * emitting a respective event.
     */
    public static class FoodSafetyDepartment extends AbstractEventReactor {

        private final List<Dish> poisonousDishes = new ArrayList<>();

        public FoodSafetyDepartment(EventBus eventBus) {
            super(eventBus);
        }

        @React
        EitherOf2<RestaurantWarningMade, RestaurantShutDown>
        makeWarning(DishFoundToBePoisonous foundToBePoisonous) {
            poisonousDishes.add(foundToBePoisonous.getDish());
            String restaurantId = foundToBePoisonous.getRestaurantId();
            if (poisonousDishes.size() >= 3) {
                String reasonFormat =
                        "The restaurant has served %s poisonous dishes and has been shut down.";
                RestaurantShutDown result = RestaurantShutDown
                        .newBuilder()
                        .setReason(format(reasonFormat, poisonousDishes.size()))
                        .setHealthInspectionId(newUuid())
                        .setRestaurantId(restaurantId)
                        .build();
                return EitherOf2.withB(result);
            } else {
                String warning = "The restaurant has been found to serve poisonous food.";
                RestaurantWarningMade result = RestaurantWarningMade
                        .newBuilder()
                        .setWarningText(warning)
                        .setRestaurantId(restaurantId)
                        .build();
                return EitherOf2.withA(result);
            }
        }

        /** Obtains a list of all dishes that have been found to be poisonous. */
        public ImmutableList<Dish> poisonousDishes() {
            return ImmutableList.copyOf(poisonousDishes);
        }
    }

    /** Throws an exception whenever a dish is cooked. */
    public static class FaultyFoodServer extends AbstractEventReactor {

        public FaultyFoodServer(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("NewExceptionWithoutArguments") /* Does not matter for testing. */
        @React
        DishServed created(DishCooked cooked) {
            throw new RuntimeException();
        }
    }

    /**
     * Oversees the proper shut down of the restaurant by escorting the staff and
     * clearing the restaurant premises.
     */
    public static class CourtOfficer extends AbstractEventReactor {

        public CourtOfficer(EventBus eventBus) {
            super(eventBus);
        }

        @React
        Pair<StaffEscorted, RestaurantPremisesCleared> shutDown(RestaurantShutDown shutDown) {
            String restaurantId = shutDown.getRestaurantId();
            StaffEscorted escorted = escortStaff(restaurantId);
            RestaurantPremisesCleared cleared = clearPremises(restaurantId);
            Pair<StaffEscorted, RestaurantPremisesCleared> result = Pair.of(escorted, cleared);
            return result;
        }

        private static RestaurantPremisesCleared clearPremises(String restaurantId) {
            RestaurantPremisesCleared result = RestaurantPremisesCleared
                    .newBuilder()
                    .setRestaurantId(restaurantId)
                    .build();
            return result;
        }

        private static StaffEscorted escortStaff(String restaurantId) {
            StaffEscorted result = StaffEscorted
                    .newBuilder()
                    .setRestaurantId(restaurantId)
                    .build();
            return result;
        }
    }

    public static class CourtOfficerOverseer extends AbstractEventSubscriber {

        private final List<String> restaurantsEscorted = new ArrayList<>();
        private final List<String> premisesCleared = new ArrayList<>();

        @Subscribe
        public void on(StaffEscorted escorted) {
            String restaurantId = escorted.getRestaurantId();
            restaurantsEscorted.add(restaurantId);
        }

        @Subscribe
        public void on(RestaurantPremisesCleared restaurantPremisesCleared) {
            String restaurantId = restaurantPremisesCleared.getRestaurantId();
            premisesCleared.add(restaurantId);
        }

        public ImmutableList<String> restaurantsEscorted() {
            return ImmutableList.copyOf(restaurantsEscorted);
        }

        public ImmutableList<String> premisesCleared() {
            return ImmutableList.copyOf(premisesCleared);
        }
    }

    /** Throws an exception whenever a dish is served. */
    public static class FaultyHealthInspector extends AbstractEventReactor {

        public FaultyHealthInspector(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("NewExceptionWithoutArguments") /* Does ont matter for testing. */
        @React(external = true)
        Optional<DishFoundToBePoisonous> inspectDish(DishServed dishServed) {
            throw new RuntimeException();
        }
    }

    /** Does not react to any events. */
    public static class IgnorantReactor extends AbstractEventReactor {

        public IgnorantReactor(EventBus eventBus) {
            super(eventBus);
        }
    }
}
