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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.test.entity.event.EntProjectCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("CompositeEventFilter should")
class CompositeEventFilterTest {

    @Test
    @DisplayName("accept any given event if no filters are provided")
    void acceptIfEmpty() {
        CompositeEventFilter emptyFilter = CompositeEventFilter
                .newBuilder()
                .build();
        Optional<? extends Message> filtered =
                emptyFilter.filter(EntProjectCreated.getDefaultInstance());
        assertTrue(filtered.isPresent());
    }

    @Test
    @DisplayName("accept the given event if all filter accept")
    void acceptIfAllAccept() {
        CompositeEventFilter filter = CompositeEventFilter
                .newBuilder()
                .add(Optional::of)
                .add(Optional::of)
                .build();
        Optional<? extends Message> filtered =
                filter.filter(EntProjectCreated.getDefaultInstance());
        assertTrue(filtered.isPresent());
    }

    @Test
    @DisplayName("not accept an event is one filter not accepts")
    void rejectIfOneRejects() {
        EventFilter spyFilter = mock(EventFilter.class);
        Message eventMessage = EntProjectCreated.getDefaultInstance();
        doReturn(Optional.of(eventMessage))
                .when(spyFilter).filter(any(Message.class));
        CompositeEventFilter filter = CompositeEventFilter
                .newBuilder()
                .add(anyEvent -> Optional.empty())
                .add(spyFilter)
                .build();
        Optional<? extends Message> filtered = filter.filter(eventMessage);
        assertFalse(filtered.isPresent());
        verifyZeroInteractions(spyFilter);
    }
}
