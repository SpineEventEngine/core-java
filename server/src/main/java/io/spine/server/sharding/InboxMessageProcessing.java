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

import io.spine.server.inbox.InboxMessage;
import io.spine.server.inbox.InboxMessage.PayloadCase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Alex Tymchenko
 */
public class InboxMessageProcessing extends ProcessingBehavior<InboxMessage> {

    @Override
    void process(List<InboxMessage> messages, List<InboxMessage> deduplicationSource) {

        Map<PayloadCase, List<InboxMessage>> byType = groupByType(messages);
        Map<PayloadCase, List<InboxMessage>> dedupSourceByType = groupByType(deduplicationSource);



    }

    private static Map<PayloadCase, List<InboxMessage>> groupByType(List<InboxMessage> messages) {
        return messages.stream()
                       .collect(Collectors.groupingBy(InboxMessage::getPayloadCase));
    }
}
