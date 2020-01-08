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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.truth.Truth8;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.DefaultRepository;
import io.spine.server.delivery.given.ConsecutiveNumberProcess;
import io.spine.server.delivery.given.ConsecutiveProjection;
import io.spine.server.delivery.given.CounterView;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventStore;
import io.spine.test.delivery.ConsecutiveNumberView;
import io.spine.test.delivery.EmitNextNumber;
import io.spine.test.delivery.NumberAdded;
import io.spine.testing.SlowTest;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.Tests.nullRef;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;

@SlowTest
@DisplayName("Catch-up of projection instances should")
public class CatchUpTest extends AbstractDeliveryTest {

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        Time.resetProvider();
    }

    @Test
    @DisplayName("catch up only particular instances by their IDs, " +
            "given the time is provided with ms resolution")
    void byIdWithMillisResolution() throws InterruptedException {
        Time.Provider provider = withMillisOnlyResolution();
        Time.setProvider(provider);

        Timestamp aWhileAgo = Timestamps.subtract(Time.currentTime(), Durations.fromHours(1));

        String[] ids = {"first", "second", "third", "fourth"};
        List<NumberAdded> events = generateEvents(200, ids);

        changeShardCountTo(2);
        CounterView.Repository repo = new CounterView.Repository();
        SingleTenantBlackBoxContext ctx = BlackBoxBoundedContext.singleTenant()
                                                                .with(repo);
        addHistory(aWhileAgo, events, ctx);

        // Round 1. Fight!

        int initialWeight = 1;
        CounterView.changeWeightTo(initialWeight);
        dispatchInParallel(ctx, events, 20, provider);

        List<Integer> initialTotals = readTotals(repo, ids);
        int sumInRound = events.size() / ids.length * initialWeight;
        IntStream sums = IntStream.iterate(sumInRound, i -> i)
                                  .limit(ids.length);
        assertThat(initialTotals).isEqualTo(sums.boxed()
                                                .collect(toList()));

        // Round 2. Catch up the first and the second and fight!

        int newWeight = 100;
        CounterView.changeWeightTo(newWeight);

        ExecutorService service = threadPoolWithTime(20, provider);
        List<Callable<Object>> jobs = new ArrayList<>();

        // Do the same, but add the catch-up for ID #0 as the first job.
        String firstId = ids[0];
        Callable<Object> firstCatchUp = () -> {
            repo.catchUp(ImmutableSet.of(firstId), aWhileAgo);
            return nullRef();
        };

        // And add the catch-up for ID #1 as the second job.
        String secondId = ids[1];
        Callable<Object> secondCatchUp = () -> {
            repo.catchUp(ImmutableSet.of(secondId), aMinuteAgo());
            return nullRef();
        };

        jobs.add(firstCatchUp);
        jobs.add(secondCatchUp);
        jobs.addAll(asPostEventJobs(ctx, events));
        service.invokeAll(jobs);
        List<Runnable> leftovers = service.shutdownNow();
        assertThat(leftovers).isEmpty();

        List<Integer> totalsAfterCatchUp = readTotals(repo, ids);

        int firstSumExpected = sumInRound * newWeight / initialWeight * 3;
        int secondSumExpected = sumInRound * newWeight / initialWeight * 2;
        int untouchedSum = sumInRound + sumInRound * newWeight / initialWeight;
        List<Integer> expectedTotals =
                ImmutableList.of(firstSumExpected, secondSumExpected, untouchedSum, untouchedSum);

        assertThat(totalsAfterCatchUp).isEqualTo(expectedTotals);
    }

    @Test
    @DisplayName("catch up all of projection instances, " +
            "given the time is provided with millisecond resolution")
    void allInOrderWithMillisResolution() throws InterruptedException {
        Time.Provider provider = withMillisOnlyResolution();
        Time.setProvider(provider);
        ConsecutiveProjection.usePositives();

        String[] ids = {"erste", "zweite", "dritte", "vierte"};
        int totalCommands = 300;
        List<EmitNextNumber> commands = generateEmissionCommands(totalCommands, ids);

        changeShardCountTo(3);
        ConsecutiveProjection.Repository projectionRepo = new ConsecutiveProjection.Repository();
        Repository<String, ConsecutiveNumberProcess> pmRepo =
                DefaultRepository.of(ConsecutiveNumberProcess.class);
        SingleTenantBlackBoxContext ctx = BlackBoxBoundedContext.singleTenant()
                                                                .with(projectionRepo)
                                                                .with(pmRepo);
        List<Callable<Object>> jobs = asPostCommandJobs(ctx, commands);
        post(jobs, withMillisOnlyResolution());

        int positiveExpected = totalCommands / ids.length;
        List<Integer> positiveValues =
                ImmutableList.of(positiveExpected, positiveExpected,
                                 positiveExpected, positiveExpected);

        List<Integer> actualLastValues = readLastValues(ids, projectionRepo);
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
        post(sameWithCatchUp, provider);

        int negativeExpected = -1 * positiveExpected * 2;

        Truth8.assertThat(projectionRepo.find(excludedTarget))
              .isEmpty();
        for (int idIndex = 1; idIndex < ids.length; idIndex++) {
            String identifier = ids[idIndex];
            Optional<ConsecutiveProjection> maybeState = projectionRepo.find(identifier);
            Truth8.assertThat(maybeState)
                  .isPresent();

            ConsecutiveNumberView state = maybeState.get()
                                                    .state();
            assertThat(state.getLastValue()).isEqualTo(negativeExpected);
        }
    }

    private static void post(List<Callable<Object>> jobs,
                             Time.Provider provider) throws InterruptedException {
        ExecutorService service = threadPoolWithTime(20, provider);
        service.invokeAll(jobs);
        List<Runnable> leftovers = service.shutdownNow();
        assertThat(leftovers).isEmpty();
    }

    private static Timestamp aMinuteAgo() {
        return Timestamps.subtract(Time.currentTime(), Durations.fromMinutes(1));
    }

    private static List<Integer> readLastValues(String[] ids,
                                                ConsecutiveProjection.Repository repo) {
        return Arrays.stream(ids)
                     .map((id) -> readLastValue(repo, id))
                     .collect(toList());
    }

    private static int readLastValue(ConsecutiveProjection.Repository repo, String id) {
        return findConsecutiveView(repo, id).state()
                                            .getLastValue();
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

    private static void addHistory(Timestamp when,
                                   List<NumberAdded> events,
                                   SingleTenantBlackBoxContext ctx) {
        EventStore eventStore = ctx.eventBus()
                                   .eventStore();
        TestEventFactory factory = TestEventFactory.newInstance(DeliveryTest.class);
        for (NumberAdded message : events) {
            Event event = factory.createEvent(message, null);
            EventContext context = event.getContext();
            EventContext modifiedContext = context.toBuilder()
                                                  .setTimestamp(when)
                                                  .vBuild();
            Event eventAtTime = event.toBuilder()
                                     .setContext(modifiedContext)
                                     .vBuild();
            eventStore.append(eventAtTime);
        }
    }

    private static List<Callable<Object>> asPostCommandJobs(SingleTenantBlackBoxContext ctx,
                                                            List<EmitNextNumber> commands) {
        return commands.stream()
                       .map(cmd -> (Callable<Object>) () -> ctx.receivesCommand(cmd))
                       .collect(toList());
    }

    private static List<Integer> readTotals(CounterView.Repository repo, String[] ids) {
        return Arrays.stream(ids)
                     .map((id) -> findCounterView(repo, id).state()
                                                           .getTotal())
                     .collect(toList());
    }

    private static List<NumberAdded> generateEvents(int howMany, String[] targets) {
        Iterator<String> idIterator = Iterators.cycle(targets);
        List<NumberAdded> events = new ArrayList<>(howMany);
        for (int i = 0; i < howMany; i++) {
            events.add(NumberAdded.newBuilder()
                                  .setCalculatorId(idIterator.next())
                                  .setValue(0)
                                  .vBuild());
        }
        return events;
    }

    private static CounterView findCounterView(CounterView.Repository repo, String id) {
        Optional<CounterView> view = repo.find(id);
        Truth8.assertThat(view)
              .isPresent();
        return view.get();
    }

    private static ConsecutiveProjection findConsecutiveView(ConsecutiveProjection.Repository repo,
                                                             String id) {
        Optional<ConsecutiveProjection> view = repo.find(id);
        if(!view.isPresent()) {
            fail(format("Cannot find the `ConsecutiveProjection` for ID `%s`.", id));
        }
        return view.get();
    }

    private static List<Callable<Object>> asPostEventJobs(SingleTenantBlackBoxContext ctx,
                                                          List<NumberAdded> events) {
        return events.stream()
                     .map(e -> (Callable<Object>) () -> ctx.receivesEvent(e))
                     .collect(toList());
    }

    private static void dispatchInParallel(SingleTenantBlackBoxContext ctx,
                                           List<NumberAdded> events,
                                           int threads,
                                           Time.Provider provider) throws InterruptedException {
        ExecutorService service = threadPoolWithTime(threads, provider);
        service.invokeAll(asPostEventJobs(ctx, events));
        List<Runnable> leftovers = service.shutdownNow();
        assertThat(leftovers).isEmpty();
    }

    private static ExecutorService threadPoolWithTime(int threadCount, Time.Provider provider) {
        ThreadFactory factory = Executors.defaultThreadFactory();
        return Executors.newFixedThreadPool(threadCount, r -> factory.newThread(() -> {
            Time.setProvider(provider);
            r.run();
        }));
    }

    private static Time.Provider withMillisOnlyResolution() {
        return () -> {
            Instant now = Instant.now();
            Timestamp result = Timestamp.newBuilder()
                                        .setSeconds(now.getEpochSecond())
                                        .setNanos(now.getNano())
                                        .build();
            return result;
        };
    }
}
