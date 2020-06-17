/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import io.spine.base.Time;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.delivery.given.ConsecutiveNumberProcess;
import io.spine.server.delivery.given.ConsecutiveProjection;
import io.spine.server.delivery.given.CounterCatchUp;
import io.spine.server.delivery.given.CounterView;
import io.spine.server.delivery.given.WhatToCatchUp;
import io.spine.server.entity.Repository;
import io.spine.test.delivery.ConsecutiveNumberView;
import io.spine.test.delivery.EmitNextNumber;
import io.spine.test.delivery.NumberAdded;
import io.spine.testing.SlowTest;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.currentTime;
import static io.spine.server.delivery.CatchUpStatus.COMPLETED;
import static io.spine.server.delivery.TestRoutines.findView;
import static io.spine.server.delivery.TestRoutines.post;
import static io.spine.server.delivery.given.WhatToCatchUp.catchUpAll;
import static io.spine.server.delivery.given.WhatToCatchUp.catchUpOf;
import static io.spine.testing.Tests.nullRef;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the {@linkplain io.spine.server.projection.ProjectionRepository#catchUp(Timestamp, Set)
 * projection catch-up} functionality.
 *
 * <p>The test routines are designed to check both small and big use-cases, including
 * the full catch-up. To deal with the different wall-clock providers, some of the tests
 * configure the {@linkplain Time#currentTime() time provider} to return the values with
 * millisecond precision. It is required to test the catch-up in the scenarios close to the legacy
 * applications, as at that time there were no emulation of the nanosecond time resolution.
 *
 * <p>As the downstream libraries, such as Spine Google Cloud library, would want to run the same
 * tests under their specific conditions, the big and slow catch-up tests are made {@code public}.
 * In this way such tests may be overridden and disabled, if needed.
 */
@SlowTest
@DisplayName("Catch-up of projection instances should")
@SuppressWarnings("WeakerAccess")   // see the class-level documentation.
public class CatchUpTest extends AbstractDeliveryTest {

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        Time.resetProvider();
    }

    @Test
    @DisplayName("given the time is provided with nanosecond resolution, catch up " +
            "only particular instances by their IDs")
    public void withNanosByIds() throws InterruptedException {
        testCatchUpByIds();
    }

    @Test
    @DisplayName("given the time is provided with nanosecond resolution, " +
            "catch up all of projection instances " +
            "and respect the order of the delivered events")
    public void withNanosAllInOrder() throws InterruptedException {
        testCatchUpAll();
    }

    @Test
    @DisplayName("given the time is provided with millisecond resolution, " +
            "catch up only particular instances by their IDs")
    public void withMillisByIds() throws InterruptedException {
        setupMillis();
        testCatchUpByIds();
    }

    @Test
    @DisplayName("given the time is provided with millisecond resolution, catch up all " +
            "of projection instances and respect the order of the delivered events")
    public void withMillisAllInOrder() throws InterruptedException {
        setupMillis();
        testCatchUpAll();
    }

    @Test
    @DisplayName("do nothing if the event store is empty")
    public void onAnEmptyStorage() {
        testCatchUpEmpty();
    }

    @Nested
    @DisplayName("allow catch-up")
    class AllowCatchUp {

        @Test
        @DisplayName("if the event store is empty")
        void onEmptyEventStore() {
            CounterCatchUp counterCatchUp = catchUpForCounter();
            counterCatchUp.catchUp(WhatToCatchUp.catchUpAll(aMinuteAgo()));
        }

        @Test
        @DisplayName("of the same instance, if the previous catch-up is already completed")
        void ifPreviousCatchUpCompleted() {
            CounterCatchUp.addOngoingCatchUpRecord(catchUpAll(aMinuteAgo()), COMPLETED);
            CounterCatchUp counterCatchUp = catchUpForCounter();
            counterCatchUp.catchUp(WhatToCatchUp.catchUpAll(aMinuteAgo()));
        }
    }

    @Nested
    @DisplayName("not allow simultaneous catch-up")
    class NotAllowSimultaneousCatchUp {

        private static final String TARGET_ID = "some target";

        @Test
        @DisplayName("if catching up of all repository instances has started previously")
        void ifCatchUpAllStartedPreviously() {
            CounterCatchUp.addOngoingCatchUpRecord(catchUpAll(aMinuteAgo()));
            CounterCatchUp counterCatchUp = catchUpForCounter();
            for (String target : counterCatchUp.targets()) {
                assertCatchUpAlreadyStarted(counterCatchUp, target);
            }
        }

        @Test
        @DisplayName("of the same repository instances")
        void ofSameInstances() {
            CounterCatchUp.addOngoingCatchUpRecord(catchUpOf(TARGET_ID, aMinuteAgo()));
            CounterCatchUp counterCatchUp = new CounterCatchUp(TARGET_ID);

            assertCatchUpAlreadyStarted(counterCatchUp, TARGET_ID);
        }

        @Test
        @DisplayName("of all instances if at least one catch-up of an instance is in progress")
        void ofAllIfOneAlreadyStarted() {
            CounterCatchUp.addOngoingCatchUpRecord(catchUpOf(TARGET_ID, aMinuteAgo()));
            CounterCatchUp counterCatchUp = new CounterCatchUp(TARGET_ID);
            try {
                counterCatchUp.catchUp(catchUpAll(aMinuteAgo()));
                fail("It must not be possible to start catching up all the instances," +
                             " while some instance is already catching up.");
            } catch (CatchUpAlreadyStartedException exception) {
                assertThat(exception.projectionStateType()).isEqualTo(CounterView.projectionType());
            }
        }

        private void assertCatchUpAlreadyStarted(CounterCatchUp counterCatchUp, String target) {
            try {
                counterCatchUp.catchUp(catchUpOf(target, aMinuteAgo()));
                fail(format("Simultaneous catch-up was somehow started for ID `%s`.", target));
            } catch (CatchUpAlreadyStartedException exception) {
                assertThat(exception.projectionStateType()).isEqualTo(CounterView.projectionType());
                assertThat(exception.requestedIds()).contains(target);
            }
        }
    }

    private static CounterCatchUp catchUpForCounter() {
        return new CounterCatchUp("first", "second", "third", "fourth");
    }

    private static void testCatchUpEmpty() {
        changeShardCountTo(17);

        CounterCatchUp counterCatchUp = catchUpForCounter();
        Timestamp aWhileAgo = subtract(currentTime(), Durations.fromHours(1));
        String someTarget = "some-target";
        counterCatchUp.catchUp(WhatToCatchUp.catchUpOf(someTarget, aWhileAgo));

        Optional<CounterView> actual = counterCatchUp.find(someTarget);
        assertThat(actual).isEmpty();
    }

    private static void testCatchUpByIds() throws InterruptedException {
        changeShardCountTo(2);

        CounterCatchUp counterCatchUp = catchUpForCounter();
        List<NumberAdded> events = counterCatchUp.generateEvents(200);

        Timestamp aWhileAgo = subtract(currentTime(), Durations.fromHours(1));
        counterCatchUp.addHistory(aWhileAgo, events);

        // Round 1. Fight!

        int initialWeight = 1;
        CounterView.changeWeightTo(initialWeight);

        counterCatchUp.dispatch(events, 20);

        String[] targets = counterCatchUp.targets();
        int totalTargets = targets.length;
        List<Integer> initialTotals = counterCatchUp.counterValues();
        int sumInRound = events.size() / totalTargets * initialWeight;
        IntStream sums = IntStream.iterate(sumInRound, i -> i)
                                  .limit(totalTargets);
        assertThat(initialTotals).isEqualTo(sums.boxed()
                                                .collect(toList()));

        // Round 2. Catch up the first and the second and fight!

        int newWeight = 100;
        CounterView.changeWeightTo(newWeight);
        counterCatchUp
                .dispatchWithCatchUp(events, 20,
                                     catchUpOf(targets[0], aWhileAgo),
                                     catchUpOf(targets[1], aMinuteAgo()));

        List<Integer> totalsAfterCatchUp = counterCatchUp.counterValues();

        int firstSumExpected = sumInRound * newWeight / initialWeight * 3;
        int secondSumExpected = sumInRound * newWeight / initialWeight * 2;
        int untouchedSum = sumInRound + sumInRound * newWeight / initialWeight;
        List<Integer> expectedTotals =
                ImmutableList.of(firstSumExpected, secondSumExpected, untouchedSum, untouchedSum);

        assertThat(totalsAfterCatchUp).isEqualTo(expectedTotals);
    }

    @SuppressWarnings("OverlyLongMethod")   // Complex environment setup.
    private static void testCatchUpAll() throws InterruptedException {
        ConsecutiveProjection.usePositives();

        String[] ids = {"erste", "zweite", "dritte", "vierte"};
        int totalCommands = 300;
        List<EmitNextNumber> commands = generateEmissionCommands(totalCommands, ids);

        changeShardCountTo(3);
        ConsecutiveProjection.Repo projectionRepo = new ConsecutiveProjection.Repo();
        Repository<String, ConsecutiveNumberProcess> pmRepo =
                DefaultRepository.of(ConsecutiveNumberProcess.class);
        BlackBoxContext ctx = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(projectionRepo)
                                     .add(pmRepo)
        );
        List<Callable<Object>> jobs = asPostCommandJobs(ctx, commands);
        post(jobs, 1);

        int positiveExpected = totalCommands / ids.length;
        List<Integer> positiveValues =
                ImmutableList.of(positiveExpected, positiveExpected,
                                 positiveExpected, positiveExpected);

        List<Integer> actualLastValues = readLastValues(projectionRepo, ids);
        assertThat(actualLastValues).isEqualTo(positiveValues);

        ConsecutiveProjection.useNegatives();

        String excludedTarget = ids[0];
        projectionRepo.excludeFromRouting(excludedTarget);

        List<Callable<Object>> sameWithCatchUp =
                ImmutableList.<Callable<Object>>builder()
                        .addAll(jobs)
                        .add(() -> {
                            projectionRepo.catchUpAll(aMinuteAgo());
                            return nullRef();
                        })
                        .build();
        post(sameWithCatchUp, 20);

        int negativeExpected = -1 * positiveExpected * 2;

        assertThat(projectionRepo.find(excludedTarget)).isEmpty();
        for (int idIndex = 1; idIndex < ids.length; idIndex++) {
            String identifier = ids[idIndex];
            Optional<ConsecutiveProjection> maybeState = projectionRepo.find(identifier);
            assertThat(maybeState).isPresent();

            ConsecutiveNumberView state = maybeState.get()
                                                    .state();
            assertThat(state.getLastValue()).isEqualTo(negativeExpected);
        }
    }

    private static Timestamp aMinuteAgo() {
        return subtract(currentTime(), Durations.fromMinutes(1));
    }

    private static List<Integer> readLastValues(ConsecutiveProjection.Repo repo,
                                                String[] ids) {
        return Arrays.stream(ids)
                     .map((id) -> findView(repo, id).state()
                                                    .getLastValue())
                     .collect(toList());
    }

    private static List<EmitNextNumber> generateEmissionCommands(int howMany, String[] ids) {
        Iterator<String> idIterator = Iterators.cycle(ids);
        List<EmitNextNumber> commands = new ArrayList<>(howMany);
        for (int i = 0; i < howMany; i++) {
            commands.add(EmitNextNumber.newBuilder()
                                       .setId(idIterator.next())
                                       .vBuild());
        }
        return commands;
    }

    private static List<Callable<Object>>
    asPostCommandJobs(BlackBoxContext ctx, List<EmitNextNumber> commands) {
        return commands.stream()
                       .map(cmd -> (Callable<Object>) () -> ctx.receivesCommand(cmd))
                       .collect(toList());
    }

    private static void setupMillis() {
        Time.setProvider(new WithMillisOnlyResolution());
    }

    /**
     * A time provider which provides the current time based upon JDK's wall clock, i.e. without
     * the emulated nanoseconds.
     */
    private static class WithMillisOnlyResolution implements Time.Provider {

        @Override
        public Timestamp currentTime() {
            Instant now = Instant.now();
            Timestamp result = Timestamp.newBuilder()
                                        .setSeconds(now.getEpochSecond())
                                        .setNanos(now.getNano())
                                        .build();
            return result;
        }
    }
}
