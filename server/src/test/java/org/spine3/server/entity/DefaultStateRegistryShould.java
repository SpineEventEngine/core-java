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

package org.spine3.server.entity;

import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Yevsyukov
 */
public class DefaultStateRegistryShould {

    private DefaultStateRegistry registry;

    @Before
    public void setUp() {
        registry = new DefaultStateRegistry();
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_same_entity_class_twice() {
        registry.put(TimerSnapshot.class, Timestamp.getDefaultInstance());
        registry.put(TimerSnapshot.class, Timestamp.getDefaultInstance());
    }

    private static class TimerSnapshot extends AbstractEntity<Long, Timestamp> {
        protected TimerSnapshot(Long id) {
            super(id);
        }
    }
}
