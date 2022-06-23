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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.spine.base.Time;
import io.spine.core.Command;
import io.spine.test.delivery.AddNumber;
import io.spine.test.delivery.Calc;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.server.delivery.DeliveryStrategy.newIndex;
import static io.spine.server.delivery.InboxIds.newSignalId;

/**
 * An abstract base for tests of {@link InboxStorage} implementations.
 */
@DisplayName("`InboxStorage` should")
public abstract class InboxStorageTest {

    private static final String TARGET_ID = "the-storage-calc";

    private final TestActorRequestFactory factory =
            new TestActorRequestFactory(InboxStorageTest.class);
    private final SecureRandom random = new SecureRandom();
    private InboxStorage storage;

    /**
     * Creates a new instance of {@code InboxStorage}.
     */
    protected abstract InboxStorage storage();

    @BeforeEach
    protected void setUp() {
        this.storage = storage();
    }

    @AfterEach
    protected void tearDown() {
        if (this.storage != null) {
            this.storage.close();
        }
    }

    @Test
    @DisplayName("write and read `InboxMessage`s")
    void writeAndReadRecords() {
        ShardIndex index = newIndex(1, 100);
        assertStorageEmpty(index);

        ImmutableList<InboxMessage> messages = generateMessages(index, 256);

        InboxMessage firstMessage = messages.get(0);

        storage.write(firstMessage);
        Page<InboxMessage> pageWithSingleRecord = readContents(index);
        assertThat(pageWithSingleRecord.contents()
                                       .size()).isEqualTo(1);
        assertThat(pageWithSingleRecord.contents()
                                       .get(0)).isEqualTo(firstMessage);

        storage.writeAll(messages.subList(1, messages.size()));
        Page<InboxMessage> pageWithAllRecords = readContents(index);
        assertSameContent(messages, pageWithAllRecords);
    }

    @Test
    @DisplayName("read `InboxMessage`s page by page starting from the older messages")
    void readRecordsPageByPage() {
        ShardIndex index = newIndex(8, 2019);
        int totalMessages = 79;
        int pageSize = 13;

        ImmutableList<InboxMessage> messages = generateMessages(index, totalMessages);
        storage.writeAll(messages);
        storage.writeAll(generateMessages(newIndex(19, 2019), 19));
        storage.writeAll(generateMessages(newIndex(21, 2019), 23));

        List<List<InboxMessage>> expected = Lists.partition(messages, pageSize);
        Page<InboxMessage> actualPage = storage.readAll(index, pageSize);
        for (Iterator<List<InboxMessage>> iterator = expected.iterator(); iterator.hasNext(); ) {
            List<InboxMessage> expectedPage = iterator.next();
            assertSameContent(expectedPage, actualPage);

            Optional<Page<InboxMessage>> maybeNext = actualPage.next();
            if (iterator.hasNext()) {
                assertThat(maybeNext).isPresent();
                actualPage = maybeNext.get();
            } else {
                assertThat(maybeNext).isEmpty();
            }
        }
    }

    /*
     * Test environment and utilities.
     *
     * @implNote Some of the package-private utilities are accessed in this section. This is why
     * it is not extracted into a separate {@code TestEnv}.
     ******************************************************************************/

    private static void assertSameContent(Collection<InboxMessage> expected,
                                          Page<InboxMessage> page) {
        assertThat(page.contents()).containsExactlyElementsIn(expected);
    }

    private ImmutableList<InboxMessage> generateMessages(ShardIndex index, int count) {
        ImmutableList.Builder<InboxMessage> msgBuilder = ImmutableList.builder();
        IntStream.range(0, count)
                 .forEach(
                         (i) -> {
                             msgBuilder.add(newCommandInInbox(index, TARGET_ID));
                             // Sleep to distinguish the messages by their `when_received` values.
                             sleepUninterruptibly(Duration.ofMillis(1));
                         }
                 );
        return msgBuilder.build();
    }

    private Page<InboxMessage> readContents(ShardIndex index) {
        return storage.readAll(index, Integer.MAX_VALUE);
    }

    private void assertStorageEmpty(ShardIndex index) {
        Page<InboxMessage> page = readContents(index);
        assertThat(page.contents()
                       .isEmpty()).isTrue();
        assertThat(page.next()).isEmpty();
    }

    private InboxMessage newCommandInInbox(ShardIndex index, String targetId) {
        Command command = factory.createCommand(AddNumber.newBuilder()
                                                         .setCalculatorId(targetId)
                                                         .setValue(random.nextInt())
                                                         .vBuild());
        InboxId inboxId = InboxIds.wrap(targetId, TypeUrl.of(Calc.class));

        InboxSignalId signalId = newSignalId(targetId, command.getId()
                                                              .value());
        InboxMessageId id = InboxMessageMixin.generateIdWith(index);
        return InboxMessage
                .newBuilder()
                .setId(id)
                .setSignalId(signalId)
                .setInboxId(inboxId)
                .setLabel(InboxLabel.HANDLE_COMMAND)
                .setStatus(InboxMessageStatus.TO_DELIVER)
                .setCommand(command)
                .setWhenReceived(Time.currentTime())
                .build();
    }
}
