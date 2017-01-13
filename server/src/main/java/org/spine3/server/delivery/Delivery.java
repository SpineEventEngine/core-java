/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.delivery;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import org.spine3.Internal;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for the strategies to deliver the packaged items to the specific consumers.
 *
 * <p>Operates with a pre-configured {@link Executor} to invoke the consumer methods.
 *
 * <p>Allows to postpone the delivery of the items on per-item-per-consumer basis.</p>
 *
 * @param <D> the type of deliverable package
 * @param <C> the type of consumer
 * @author Alex Tymchenko
 */
@Internal
public abstract class Delivery<D, C> {

    private final Executor delegate;

    /**
     * Creates a delivery strategy with a specified {@link Executor} used for the operation.
     */
    protected Delivery(Executor delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates an instance of a delivery with a {@link MoreExecutors#directExecutor()} used for the operation.
     */
    protected Delivery() {
        this(MoreExecutors.directExecutor());
    }

    /**
     * Determines whether the item delivery should be postponed for the given {@code deliverable} and {@code consumer}.
     *
     * <p>This method must be implemented in descendants.
     *
     * @param deliverable the deliverable item which delivery may potentially be postponed
     * @param consumer    the target consumer for the deliverable
     * @return {@code true}, if the delivery should be postponed, {@code false} otherwise.
     */
    protected abstract boolean shouldPostponeDelivery(D deliverable, C consumer);

    /**
     * Defines what should be done to deliver a given {@code deliverable} to the {@code targetConsumer}.
     *
     * <p>Each {@code Delivery} implementation defines this behaviour depending on the type of the consumer.
     *
     * <p>The result is defined as {@link Runnable}, as this action will be executed by a delegate {@code Executor},
     * according to the strategy implementation regulated by {@link #shouldPostponeDelivery(Object, Object)} and
     * {@link #deliverNow(Object, Class)} methods.
     *
     * @param consumer    the consumer to deliver the item to
     * @param deliverable the item to be delivered
     * @return the action on how to deliver the given item to the consumer.
     */
    protected abstract Runnable getDeliveryAction(C consumer, D deliverable);

    /**
     * Determines a collection of the consumers for the given {@code deliverable}.
     *
     * @param deliverable the item to deliver
     * @return the collection of the consumers.
     */
    protected abstract Collection<C> consumersFor(D deliverable);

    /**
     * Passes the deliverable to the matching consumers using the {@code executor} configured for this instance
     * of {@code Delivery}.
     *
     * <p>The delivery of the item to each of the consumers may be postponed according to
     * {@link #shouldPostponeDelivery(Object, Object)} invocation result.
     *
     * @param deliverable the item to deliver
     */
    @Internal           // It is `public` as long as it is accessed from various framework packages.
    public void deliver(D deliverable) {
        final Collection<C> consumers = consumersFor(deliverable);
        for (C consumer : consumers) {
            final boolean shouldPostpone = shouldPostponeDelivery(deliverable, consumer);
            if (!shouldPostpone) {
                deliverNow(deliverable, consumer.getClass());
            }
        }
    }

    /**
     * Delivers the item immediately.
     *
     * @param deliverable   the item, packaged for the delivery
     * @param consumerClass the class of the target consumer
     */
    @SuppressWarnings("WeakerAccess")       // Part of API.
    public void deliverNow(final D deliverable, final Class<?> consumerClass) {
        final Collection<C> consumers = consumersFor(deliverable);
        final Iterable<C> matching = Iterables.filter(consumers, matchClass(consumerClass));

        for (final C targetConsumer : matching) {
            final Runnable deliveryAction = getDeliveryAction(targetConsumer, deliverable);
            execute(deliveryAction);
        }
    }

    private void execute(Runnable command) {
        delegate.execute(command);
    }

    private static <T> Predicate<T> matchClass(final Class<? extends T> classToMatch) {
        return new Predicate<T>() {
            @Override
            public boolean apply(@Nullable T input) {
                checkNotNull(input);
                return classToMatch.equals(input.getClass());
            }
        };
    }

}
