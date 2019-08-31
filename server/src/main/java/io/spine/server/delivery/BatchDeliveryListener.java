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

package io.spine.server.delivery;

/**
 * Listens to start and end of the {@code Inbox} batch delivery.
 *
 * <p>When the consequent messages in the {@code Inbox} are targeting the same entity,
 * the delivery may be optimized by using either the same transaction or caching the storage
 * operations while the batch is delivered.
 *
 * <p>The implementing classes may define their own behavior and react upon such use cases.
 */
public interface BatchDeliveryListener<I> {

    /**
     * Invoked before the batch delivery to the target with the given ID is started.
     *
     * @param id
     *         the ID of the delivery target
     */
    void onStart(I id);

    /**
     * Invoked after the batch delivery to the target with the given ID is ended.
     *
     * @param id
     *         the ID of the delivery target
     */
    void onEnd(I id);
}
