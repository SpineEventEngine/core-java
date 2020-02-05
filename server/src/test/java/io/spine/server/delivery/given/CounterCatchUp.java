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

package io.spine.server.delivery.given;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.CatchUp;
import io.spine.server.delivery.CatchUpId;
import io.spine.server.delivery.CatchUpStatus;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.LocalDispatchingObserver;
import io.spine.server.event.EventStore;
import io.spine.server.storage.memory.InMemoryCatchUpStorage;
import io.spine.test.delivery.NumberAdded;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.delivery.TestRoutines.findView;
import static io.spine.server.delivery.TestRoutines.post;
import static io.spine.testing.Tests.nullRef;
import static java.util.stream.Collectors.toList;

/**
 * A convenience wrapper over the {@link CounterView} repository and the BlackBox Bounded Context
 * to be used in the catch-up tests.
 */
public class CounterCatchUp {

    private final CounterView.Repository repo;
    private final SingleTenantBlackBoxContext ctx;
    private final String[] ids;

    public CounterCatchUp(String... ids) {
        this.ids = ids.clone();
        this.repo = new CounterView.Repository();
        this.ctx = BlackBoxBoundedContext.singleTenant()
                                         .with(repo);
    }

    public void addHistory(Timestamp when, List<NumberAdded> events) {
        EventStore eventStore = ctx.eventBus()
                                   .eventStore();
        TestEventFactory factory = TestEventFactory.newInstance(getClass());
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

    public void dispatch(List<NumberAdded> events, int threads)
            throws InterruptedException {
        post(asPostEventJobs(ctx, events), threads);
    }

    public List<Integer> counterValues() {
        return Arrays.stream(ids)
                     .map((id) -> findView(repo, id).state()
                                                    .getTotal())
                     .collect(toList());
    }

    public String[] targets() {
        return ids.clone();
    }

    public List<NumberAdded> generateEvents(int howMany) {
        Iterator<String> idIterator = Iterators.cycle(ids);
        List<NumberAdded> events = new ArrayList<>(howMany);
        for (int i = 0; i < howMany; i++) {
            events.add(NumberAdded.newBuilder()
                                  .setCalculatorId(idIterator.next())
                                  .setValue(0)
                                  .vBuild());
        }
        return events;
    }

    public void dispatchWithCatchUp(List<NumberAdded> events, int threads, WhatToCatchUp... whatToCatchUp)
            throws InterruptedException {
        List<Callable<Object>> jobs = new ArrayList<>();
        jobs.addAll(asCallableJobs(whatToCatchUp));
        jobs.addAll(asPostEventJobs(ctx, events));
        post(jobs, threads);
    }

    private ImmutableList<Callable<Object>> asCallableJobs(WhatToCatchUp... whatToCatchUp) {
        ImmutableList.Builder<Callable<Object>> jobs = ImmutableList.builder();
        for (WhatToCatchUp task : whatToCatchUp) {
            Callable<Object> callable = () -> {
                catchUp(task);
                return nullRef();
            };
            jobs.add(callable);
        }
        return jobs.build();
    }

    public void catchUp(WhatToCatchUp task) {
        if (task.shouldCatchUpAll()) {
            repo.catchUpAll(task.sinceWhen());
        } else {
            String targetId = checkNotNull(task.id());
            repo.catchUp(task.sinceWhen(), ImmutableSet.of(targetId));
        }
    }

    private static List<Callable<Object>> asPostEventJobs(SingleTenantBlackBoxContext ctx,
                                                          List<NumberAdded> events) {
        return events.stream()
                     .map(e -> (Callable<Object>) () -> ctx.receivesEvent(e))
                     .collect(toList());
    }

    public static void addOngoingCatchUpRecord(WhatToCatchUp target) {
        addOngoingCatchUpRecord(target, CatchUpStatus.STARTED);
    }

    public static void addOngoingCatchUpRecord(WhatToCatchUp target, CatchUpStatus status) {
        InMemoryCatchUpStorage storage = new InMemoryCatchUpStorage(false);
        CatchUpId catchUpId = CatchUpId.newBuilder()
                                       .setUuid(Identifier.newUuid())
                                       .setProjectionType(CounterView.projectionType()
                                                                     .value())
                                       .build();
        CatchUp.Request.Builder requestBuilder = CatchUp.Request.newBuilder()
                                                                .setSinceWhen(target.sinceWhen());
        if (!target.shouldCatchUpAll()) {
            String identifier = checkNotNull(target.id());
            requestBuilder.addTarget(Identifier.pack(identifier));
        }
        CatchUp.Request allTargetsMinuteAgo = requestBuilder.build();

        CatchUp existingState = CatchUp.newBuilder()
                                       .setId(catchUpId)
                                       .setStatus(status)
                                       .setRequest(allTargetsMinuteAgo)
                                       .vBuild();
        storage.write(existingState);
        Delivery delivery = Delivery.newBuilder()
                                    .setCatchUpStorage(storage)
                                    .build();
        delivery.subscribe(new LocalDispatchingObserver());
        ServerEnvironment.instance()
                         .configureDelivery(delivery);
    }
}
