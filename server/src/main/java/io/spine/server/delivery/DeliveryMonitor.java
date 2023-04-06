/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import io.spine.annotation.SPI;
import io.spine.server.NodeId;
import io.spine.server.delivery.FailedReception.Action;

/**
 * A controlling monitor of a {@link Delivery Inbox Delivery} process.
 *
 * <p>In some cases when a funnel-like {@code Entity} (e.g. some projection of a business report
 * subscribed to half of the domain events) has a lot of messages in its {@code Inbox},
 * the delivery process may be slowed down for other targets in the same shard, while
 * all the messages are being dispatched the "busy" {@code Entity}. Depending on the number
 * of messages to deliver, it may take a lot of time.
 *
 * <p>Environments such as Google AppEngine Standard imply restrictions on the duration of the
 * request processing. Therefore, it may be necessary to stop the message delivery for this shard
 * at some point, and re-schedule the shard processing again by sending another request and
 * thus resetting the processing clock.
 */
@SPI
public class DeliveryMonitor {

    private static final DeliveryMonitor ALWAYS_CONTINUE = new DeliveryMonitor();

    /**
     * Determines if the delivery execution should be continued after the given stage is completed.
     *
     * <p>If {@code false} is returned, the ongoing delivery run for the served shard will be
     * stopped at this application node. The node will release the previously
     * {@linkplain ShardedWorkRegistry#pickUp(ShardIndex, NodeId) picked up} shard.
     *
     * <p>To trigger the new delivery processing for this shard, use
     * {@linkplain Delivery#deliverMessagesFrom(ShardIndex)
     * Delivery.deliverMessagesFrom(ShardIndex)}.
     *
     * <p>This method is called synchronously, meaning that the delivery process will not
     * resume until a value is returned from this method call.
     *
     * @param stage
     *         the stage of delivery which has ended
     * @return {@code true} to continue the delivery, {@code false} to stop
     * @implNote The default implementation stops the execution once there were zero
     *         messages {@linkplain DeliveryStage#getMessagesDelivered() delivered in
     *         the given stage}.
     */
    public boolean shouldContinueAfter(DeliveryStage stage) {
        return true;
    }

    /**
     * Called once some delivery process has completed and the corresponding shard
     * has been released.
     *
     * <p>The descendants may override this method to understand when it is safe to pick up
     * the corresponding shard again. Another usage scenario is calculation of the message delivery
     * throughput.
     *
     * @param stats
     *         the statistics of the performed delivery
     */
    @SuppressWarnings("unused")  // This SPI method is designed for descendants.
    public void onDeliveryCompleted(DeliveryStats stats) {
        // Do nothing.
    }

    @SuppressWarnings({"WeakerAccess", "unused"})   /* Part of public API. */
    public void onDeliveryStarted(ShardIndex index) {
        // Do nothing.
    }

    /**
     * Called if an {@code Exception} occurred when the {@code Delivery} attempted
     * to pick up a shard.
     *
     * <p>Please note, this callback is <em>not</em> invoked in case the shard cannot be picked
     * if it's already picked by another worker. It is so, because such a use case is a part
     * of normal flow, and thus does not provoke a {@code RuntimeException}.
     *
     * <p>Returns an action to take in relation to the failure.
     *
     * <p>By default this callback returns an Action that propagates the occurred exception.
     * This means the occurred exception will be throw out of
     * the {@link Delivery#deliverMessagesFrom(ShardIndex) deliverMessagesFrom()} method.
     *
     * @param failure
     *         contains an information about the occurred failure and gives access to
     *         predefined {@code Action}s to handle the error.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})  /* Part of public API. */
    public FailedPickUp.Action onShardPickUpFailure(RuntimeFailure failure) {
        return failure.propagate();
    }

    /**
     * Called if {@code Delivery} could not pick up a shard because it was already picked
     * by another worker.
     *
     * <p>Returns an action to take in relation to the failure.
     *
     * <p>By default this callback returns an Action that does nothing. This means that
     * an empty {@code Optional} will be returned from the {@code deliverMessagesFrom()} method.
     *
     * @param failure
     *         contains an information about the worker who owns the session and gives access
     *         to predefined {@code Action}s to handle the error.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})  /* Part of public API. */
    public FailedPickUp.Action onShardAlreadyPicked(AlreadyPickedUp failure) {
        return failure.doNothing();
    }

    /**
     * A callback invoked if the signal transmitted via given message
     * was handled by the respective receptor with failure.
     *
     * <p>Returns an action to take in relation to the failure.
     *
     * <p>By default, this callback returns an action which
     * marks the message as {@linkplain InboxMessageStatus#DELIVERED delivered}.
     *
     * <p>See {@link FailedReception} for more pre-defined actions.
     *
     * @param reception
     *         the details on failed reception
     */
    @SuppressWarnings("WeakerAccess")   /* Part of public API. */
    public Action onReceptionFailure(FailedReception reception) {
        return reception.markDelivered();
    }

    /**
     * Returns an instance of {@code DeliveryMonitor} which always tells to continue.
     */
    static DeliveryMonitor alwaysContinue() {
        return ALWAYS_CONTINUE;
    }
}
