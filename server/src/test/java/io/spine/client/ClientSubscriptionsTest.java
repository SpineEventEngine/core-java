/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.tasks.CTask;
import io.spine.test.client.tasks.CTaskId;
import io.spine.test.client.tasks.command.ArchiveCTask;
import io.spine.test.client.tasks.command.CreateCTask;
import io.spine.test.client.tasks.command.DeleteCTask;
import io.spine.test.client.tasks.command.RestoreCTask;
import io.spine.test.client.tasks.command.UnarchiveCTask;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.client.given.ClientTasksTestEnv.archiveCTask;
import static io.spine.client.given.ClientTasksTestEnv.createCTask;
import static io.spine.client.given.ClientTasksTestEnv.deleteCTask;
import static io.spine.client.given.ClientTasksTestEnv.restoreCTask;
import static io.spine.client.given.ClientTasksTestEnv.stateAfter;
import static io.spine.client.given.ClientTasksTestEnv.unarchiveCTask;
import static io.spine.test.client.ClientTestContext.tasks;
import static java.time.Duration.ofSeconds;

@MuteLogging
@DisplayName("Subscription API for `Client` should allow subscribe to ")
final class ClientSubscriptionsTest extends AbstractClientTest {

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(tasks());
    }

    @DisplayName("changes")
    @Nested
    class Changes {

        @Test
        @DisplayName("in state of an Aggregate")
        void aggregate() {
            AtomicBoolean updateReceived = new AtomicBoolean(false);
            Client client = client();

            CreateCTask createTask = createCTask("My task");
            CTask expectedState = stateAfter(createTask);
            client.asGuest()
                  .subscribeTo(CTask.class)
                  .observe(update -> {
                      updateReceived.set(true);
                      assertThat(update)
                              .comparingExpectedFieldsOnly()
                              .isEqualTo(expectedState);
                  })
                  .post();
            postAndForget(client, createTask);
            sleepUninterruptibly(ofSeconds(1));
            assertThat(updateReceived.get())
                    .isTrue();
        }
    }

    @DisplayName("deletion and restoration")
    @Nested
    class Deletion {

        @Test
        @DisplayName("of an Aggregate")
        void aggregate() {
            List<CTask> receivedUpdates = new ArrayList<>();

            Client client = client();
            CreateCTask createTask = createCTask("Soon to be deleted and restored");
            CTaskId id = createTask.getId();
            DeleteCTask deleteTask = deleteCTask(id);
            RestoreCTask restoreTask = restoreCTask(id);
            client.asGuest()
                  .subscribeTo(CTask.class)
                  .observe(receivedUpdates::add)
                  .post();

            assertNumOfUpdates(receivedUpdates, 1, client, createTask);
            assertNumOfUpdates(receivedUpdates, 1, client, deleteTask);
            assertNumOfUpdates(receivedUpdates, 2, client, restoreTask);
        }
    }

    @DisplayName("archiving and un-archiving")
    @Nested
    class Archiving {

        @Test
        @DisplayName("of an Aggregate")
        void aggregate() {
            List<CTask> receivedUpdates = new ArrayList<>();

            Client client = client();
            CreateCTask createTask = createCTask("Soon to be archived and un-archived");
            CTaskId id = createTask.getId();
            ArchiveCTask archiveTask = archiveCTask(id);
            UnarchiveCTask unarchiveCTask = unarchiveCTask(id);
            client.asGuest()
                  .subscribeTo(CTask.class)
                  .observe(receivedUpdates::add)
                  .post();

            assertNumOfUpdates(receivedUpdates, 1, client, createTask);
            assertNumOfUpdates(receivedUpdates, 1, client, archiveTask);
            assertNumOfUpdates(receivedUpdates, 2, client, unarchiveCTask);
        }
    }

    private static void postAndForget(Client client, CommandMessage createTask) {
        client.asGuest()
              .command(createTask)
              .postAndForget();
    }

    private static void assertNumOfUpdates(List<CTask> updates, int expectedCount,
                                           Client client, CommandMessage cmd) {
        postAndForget(client, cmd);
        sleepUninterruptibly(ofSeconds(1));
        assertThat(updates.size())
                .isEqualTo(expectedCount);
    }
}
