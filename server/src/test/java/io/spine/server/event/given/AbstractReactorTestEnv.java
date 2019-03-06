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
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.EventBus;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf3;
import io.spine.test.event.CharityDonationOffered;
import io.spine.test.event.ChefPraised;
import io.spine.test.event.ChefWarningSent;
import io.spine.test.event.Dish;
import io.spine.test.event.DishCooked;
import io.spine.test.event.DishFoundToBePoisonous;
import io.spine.test.event.DishReturnedToKitchen;
import io.spine.test.event.DishReviewLeft;
import io.spine.test.event.DishServed;
import io.spine.test.event.FoodDelivered;
import io.spine.test.event.UserNotified;
import io.spine.test.event.UserNotified.NotificationMethod;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static java.lang.String.format;

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
    public static DishServed dishServed(Dish dishToServe, String restaurantId) {
        DishServed result = DishServed
                .newBuilder()
                .setDish(dishToServe)
                .setRestaurantId(restaurantId)
                .build();
        return result;
    }

    /** Returns an event that signifies that the specified dish has been delivered. */
    public static FoodDelivered foodDelivered(Dish dishToDeliver) {
        FoodDelivered result = FoodDelivered
                .newBuilder()
                .setDish(dishToDeliver)
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

    /** Obtains an event that signifies that a perfect review was left about the specified dish. */
    public static DishReviewLeft exceptionalReviewLeft(Dish dish) {
        DishReviewLeft result = DishReviewLeft
                .newBuilder()
                .setDish(dish)
                .setScore(10)
                .build();
        return result;
    }

    /** Obtains an event that signifies that a negative review was left about the specified dish. */
    public static DishReviewLeft badReviewLeft(Dish dish) {
        DishReviewLeft result = DishReviewLeft
                .newBuilder()
                .setDish(dish)
                .setScore(3)
                .build();
        return result;
    }

    /** Notifies user that the dish has been served and is ready for delivery. */
    public static class DeliveryNotifier extends AbstractEventReactor {

        private final List<UserNotified> notificationsSent = new ArrayList<>();
        private final NotificationMethod notificationMethod;

        public DeliveryNotifier(EventBus eventBus, NotificationMethod notificationMethod) {
            super(eventBus);
            this.notificationMethod = notificationMethod;
        }

        @React
        UserNotified notify(DishCooked cooked) {
            String dishName = cooked.getDish()
                                    .getDishName();
            String message = format("Dish `%s` is on its way.", dishName);

            UserNotified result = UserNotified
                    .newBuilder()
                    .setNotificationMessage(message)
                    .setNotificationMethod(notificationMethod)
                    .build();
            notificationsSent.add(result);
            return result;
        }

        public ImmutableList<UserNotified> notificationsSent() {
            return ImmutableList.copyOf(notificationsSent);
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
     * Praises the chef whenever a positive dish review was left by emitting a
     * respective event.
     */
    public static class ChefPerformanceTracker extends AbstractEventReactor {

        private final DoubleSummaryStatistics chefStats = new DoubleSummaryStatistics();
        private static final String PRAISE_MESSAGE_FORMAT = "Customer has left a review" +
                "of %s out of 10 for %s. Good job! Your average score is `%s`.";

        private static final String WARNING_MESSAGE_FORMAT = "Your score is %s, it has dipped" +
                "below 5. Try harder.";

        public ChefPerformanceTracker(EventBus eventBus) {
            super(eventBus);
        }

        @React
        EitherOf3<ChefPraised, ChefWarningSent, Nothing> acceptReview(DishReviewLeft reviewLeft) {
            int score = reviewLeft.getScore();
            chefStats.accept(score);
            if (score >= 8) {
                Dish dish = reviewLeft.getDish();
                String dishName = dish.getDishName();
                double currentAverage = chefStats.getAverage();
                String message = format(PRAISE_MESSAGE_FORMAT, score, dishName, currentAverage);
                ChefPraised chefPraised = praiseChef(dish, message, score);
                return EitherOf3.withA(chefPraised);
            }
            if (chefStats.getAverage() <= 3) {
                ChefWarningSent warningSent = warnChef();
                return EitherOf3.withB(warningSent);
            }
            return EitherOf3.withC(nothing());
        }

        private ChefWarningSent warnChef() {
            ChefWarningSent result = ChefWarningSent
                    .newBuilder()
                    .setMessage(format(WARNING_MESSAGE_FORMAT, chefStats.getAverage()))
                    .build();
            return result;
        }

        private static ChefPraised praiseChef(Dish dish, String message, int score) {
            ChefPraised result = ChefPraised
                    .newBuilder()
                    .setDish(dish)
                    .setMessage(message)
                    .setUserReview(score)
                    .build();
            return result;
        }

        /**
         * Obtains the stats of the scores of the chef, i.e. amount of reviews and the average
         * score.
         */
        public DoubleSummaryStatistics chefStats() {
            return chefStats;
        }
    }

    /** Offers to donate to charity each time a dish is paid for by emitting a respective event. */
    public static class CharityAgent extends AbstractEventReactor {

        private static final String MESSAGE = "Would you like to donate to charity?";

        private int offersMade = 0;

        public CharityAgent(EventBus eventBus) {
            super(eventBus);
        }

        @React(external = true)
        CharityDonationOffered offerToDonate(DishServed served) {
            return offerDonation();
        }

        @React(external = true)
        CharityDonationOffered offerToDonate(FoodDelivered delivered) {
            return offerDonation();
        }

        private CharityDonationOffered offerDonation() {
            CharityDonationOffered result = CharityDonationOffered
                    .newBuilder()
                    .setMessage(MESSAGE)
                    .build();
            offersMade++;
            return result;
        }

        public int offersMade() {
            return offersMade;
        }
    }

    /** In an attempt to offer a charity donation, stutters and throws an exception. */
    public static class StutteringCharityAgent extends AbstractEventReactor {

        public StutteringCharityAgent(EventBus eventBus) {
            super(eventBus);
        }

        @React(external = true)
        CharityDonationOffered offerToDonate(DishServed served) {
            return stutter();
        }

        @SuppressWarnings("NewExceptionWithoutArguments") /* Does not matter for this test case. */
        private static CharityDonationOffered stutter() {
            throw new RuntimeException();
        }
    }

    /** Throws an exception whenever a dish is cooked. */
    public static class FaultyDeliveryNotifier extends AbstractEventReactor {

        public FaultyDeliveryNotifier(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("NewExceptionWithoutArguments")/* Does not matter for this test case. */
        @React
        UserNotified notifyUser(DishCooked cooked) {
            throw new RuntimeException();
        }
    }
}

