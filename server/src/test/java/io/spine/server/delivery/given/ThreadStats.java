/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The statistics of threads, in which the entity has been processed.
 *
 * <p>Required in order to verify the sharding configuration.
 *
 * @param <I> the type of entity identifiers
 * @author Alex Tymchenko
 */
public class ThreadStats<I extends Message> {

    private final Multimap<Long, I> threadToId =
            Multimaps.synchronizedMultimap(HashMultimap.create());

    public void recordCallingThread(I id) {
        long currentThreadId = Thread.currentThread()
                                     .getId();
        threadToId.put(currentThreadId, id);
    }

    public void assertIdCount(int expectedSize) {
        int actualSize = threadToId.size();
        assertEquals(expectedSize, actualSize);
    }

    public void assertThreadCount(int expectedCount) {
        int actualCount = threadToId.keySet()
                                    .size();
        assertEquals(expectedCount, actualCount);
    }

    public void clear() {
        threadToId.clear();
    }
}
