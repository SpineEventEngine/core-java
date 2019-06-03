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

package io.spine.server.sharding;

import java.util.List;

/**
 * A behavior of postponed processing the previously sharded messages.
 *
 * @param <M>
 *         the type of sharded messages
 * @author Alex Tymchenko
 */
public abstract class ProcessingBehavior<M extends ShardedRecord> {

    /**
     * Sends the postponed messages for processing.
     *
     * <p>The descendants typically will load the targets for the messages
     * and handle the dispatching results.
     *
     * <p>Any runtime issues should be handled by the descendants by emitting the corresponding
     * rejection events and potentially notifying the respective entity repositories.
     *
     * <p>The messages passed as {@code deduplicationSource} reside in
     * {@code now - de-duplication depth} timeframe. In most of cases, the set of messages sent
     * for processing are included in the set of those to be used for de-duplication.
     *
     * @param toProcess
     *         the messages to send for processing
     * @param deduplicationSource
     *         the messages to look for duplicates amongst
     */
    protected abstract void process(List<M> toProcess, List<M> deduplicationSource);
}
