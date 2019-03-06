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

import io.spine.logging.Logging;
import io.spine.server.event.given.AbstractReactorTestEnv.CharityAgent;
import io.spine.server.event.given.AbstractReactorTestEnv.ChefPerformanceTracker;
import io.spine.server.event.given.AbstractReactorTestEnv.DeliveryNotifier;
import io.spine.server.event.given.AbstractReactorTestEnv.FaultyDeliveryNotifier;
import io.spine.server.event.given.AbstractReactorTestEnv.StutteringCharityAgent;
import io.spine.test.event.CharityDonationOffered;
import io.spine.test.event.ChefPraised;
import io.spine.test.event.Dish;
import io.spine.test.event.DishCooked;
import io.spine.test.event.DishReviewLeft;
import io.spine.test.event.DishServed;
import io.spine.test.event.FoodDelivered;
import io.spine.test.event.UserNotified;
import io.spine.test.event.UserNotified.NotificationMethod;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.ArrayDeque;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Queue;

import static io.spine.base.Identifier.newUuid;
import static io.spine.server.event.given.AbstractReactorTestEnv.badReviewLeft;
import static io.spine.server.event.given.AbstractReactorTestEnv.dishServed;
import static io.spine.server.event.given.AbstractReactorTestEnv.exceptionalReviewLeft;
import static io.spine.server.event.given.AbstractReactorTestEnv.foodDelivered;
import static io.spine.server.event.given.AbstractReactorTestEnv.poisonousDish;
import static io.spine.server.event.given.AbstractReactorTestEnv.someDish;
import static io.spine.test.event.UserNotified.NotificationMethod.SMS;
import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static java.util.Collections.nCopies;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.slf4j.event.Level.ERROR;

@DisplayName("Abstract event reactor should")
class AbstractEventReactorTest {

    private static final NotificationMethod DESIRED_NOTIFICATION_METHOD = SMS;

    private BlackBoxBoundedContext<SingleTenantBlackBoxContext> restaurantContext;
    private BlackBoxBoundedContext<SingleTenantBlackBoxContext> deliveryContext;
    private BlackBoxBoundedContext<SingleTenantBlackBoxContext> charityContext;

    private CharityAgent charityAgent;
    private ChefPerformanceTracker chefPerformanceTracker;
    private DeliveryNotifier notifier;

    AbstractEventReactorTest() {
    }

    @BeforeEach
    void setUp() {
        restaurantContext = BlackBoxBoundedContext.singleTenant();
        notifier = new DeliveryNotifier(restaurantContext.eventBus(), DESIRED_NOTIFICATION_METHOD);
        chefPerformanceTracker = new ChefPerformanceTracker(restaurantContext.eventBus());
        restaurantContext.registerEventDispatchers(notifier);
        restaurantContext.registerEventDispatchers(chefPerformanceTracker);

        charityContext = BlackBoxBoundedContext.singleTenant();
        charityAgent = new CharityAgent(charityContext.eventBus());
        charityContext.registerEventDispatchers(charityAgent);

        deliveryContext = BlackBoxBoundedContext.singleTenant();
    }

    @Test
    @DisplayName("throw upon a null event bus")
    void throwOnNullEventBus() {
        assertThrows(NullPointerException.class, () -> new CharityAgent(null));
    }

    @DisplayName("while dealing with domestic events")
    @Nested
    class DomesticEvents {

        @Test
        @DisplayName("receive one")
        void receive() {
            Dish dishToCook = someDish();
            DishCooked dishCooked = DishCooked
                    .newBuilder()
                    .setDish(dishToCook)
                    .build();

            restaurantContext.receivesEvent(dishCooked);
            boolean exactlyOneReceived = notifier.notificationsSent()
                                                 .size() == 1;
            UserNotified notification = notifier.notificationsSent()
                                                .get(0);
            boolean ofCorrectType =
                    notification.getNotificationMethod() == DESIRED_NOTIFICATION_METHOD;

            assertTrue(exactlyOneReceived && ofCorrectType);
        }

        @Test
        @DisplayName("receive several")
        void receiveSeveral() {
            Dish dishToReview = someDish();
            DishReviewLeft exceptionalReview = exceptionalReviewLeft(dishToReview);
            DishReviewLeft badReview = badReviewLeft(dishToReview);
            restaurantContext.receivesEvents(exceptionalReview, badReview);

            double expectedScore = (exceptionalReview.getScore() + badReview.getScore()) * 0.5;
            DoubleSummaryStatistics stats = chefPerformanceTracker.chefStats();
            boolean twoReviewsReceived = stats.getCount() == 2;
            assertTrue(twoReviewsReceived);
            assertEquals(expectedScore, stats.getAverage());
        }

        @Test
        @DisplayName("log an error")
        void logError() {
            FaultyDeliveryNotifier faultyNotifier =
                    new FaultyDeliveryNotifier(restaurantContext.eventBus());
            restaurantContext.registerEventDispatchers(faultyNotifier);
            Queue<SubstituteLoggingEvent> loggedMessages =
                    redirectLogging((SubstituteLogger) faultyNotifier.log());

            DishCooked cooked = DishCooked
                    .newBuilder()
                    .setDish(someDish())
                    .build();
            restaurantContext.receivesEvent(cooked);
            assertLoggedCorrectly(loggedMessages);
        }

        /**
         * See {@link ChefPerformanceTracker#praiseChef(DishReviewLeft)}.
         */
        @Test
        @DisplayName("react on one")
        void react() {
            Dish dishToReview = someDish();
            restaurantContext.receivesEvents(exceptionalReviewLeft(dishToReview),
                                             badReviewLeft(dishToReview),
                                             badReviewLeft(dishToReview))
                             .assertThat(emittedEvent(ChefPraised.class, once()));
        }

        @Test
        @DisplayName("react on several")
        void reactOnSeveral() {
            Dish dishToReview = someDish();
            int expectedPraisesAmount = 5;
            List<DishReviewLeft> positiveReviews = nCopies(5, exceptionalReviewLeft(dishToReview));
            positiveReviews.forEach(restaurantContext::receivesEvent);
            restaurantContext.assertThat(emittedEvent(ChefPraised.class,
                                                      count(expectedPraisesAmount)));
        }

        @Test
        @DisplayName("react with either of some events")
        void reactWithSeveral() {
            Dish dishToReview = someDish();
            DishReviewLeft exceptionalReview = exceptionalReviewLeft(dishToReview);
            restaurantContext.receivesEvent(exceptionalReview)
                             .assertThat(emittedEvent(ChefPraised.class, once()));

            List<DishReviewLeft> badReviews = nCopies(3, badReviewLeft(dishToReview));
            badReviews.forEach(restaurantContext::receivesEvent);
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
            charityContext.receivesExternalEvent(restaurantContext.name(), served);
            assertEquals(1, charityAgent.offersMade());
        }

        @DisplayName("react to one")
        @Test
        void reactToOne() {
            Dish dishToServe = someDish();
            DishServed dishServed = dishServed(dishToServe, newUuid());

            charityContext.receivesExternalEvent(charityContext.name(), dishServed)
                          .assertThat(emittedEvent(CharityDonationOffered.class, once()));
        }

        @DisplayName("react to several")
        @Test
        void reactToSeveral() {
            Dish dishToServe = someDish();
            DishServed dishServed = dishServed(dishToServe, newUuid());

            Dish dishToDeliver = someDish();
            FoodDelivered foodDelivered = foodDelivered(dishToDeliver);

            charityContext.receivesExternalEvent(restaurantContext.name(), dishServed)
                          .receivesExternalEvent(deliveryContext.name(), foodDelivered)
                          .assertThat(emittedEvent(CharityDonationOffered.class, twice()));
        }

        @DisplayName("log an error")
        @Test
        void logAnError() {
            StutteringCharityAgent stutteringAgent =
                    new StutteringCharityAgent(charityContext.eventBus());
            charityContext.registerEventDispatchers(stutteringAgent);

            Queue<SubstituteLoggingEvent> loggedMessages = redirectLogging(
                    (SubstituteLogger) stutteringAgent.log());
            Dish dishToServe = someDish();
            DishServed dishServed = DishServed
                    .newBuilder()
                    .setDish(dishToServe)
                    .build();
            charityContext.receivesExternalEvent(restaurantContext.name(), dishServed);
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
     *     <li>only 1 message has been logged;
     *     <li>logged message is of the {@code ERROR} level.
     * </ul>
     */
    private static void assertLoggedCorrectly(Queue<SubstituteLoggingEvent> messages) {
        assertEquals(1, messages.size());
        SubstituteLoggingEvent loggedWarning = messages.poll();
        assertEquals(ERROR, loggedWarning.getLevel());
    }
}
