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

package io.spine.testing.server.blackbox.verify.subscription;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.extensions.proto.ProtoSubject;
import io.spine.client.SubscriptionUpdate;
import io.spine.testing.server.blackbox.BbProject;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.given.GivenSubscriptionUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

@DisplayName("ToProtoSubjects should")
class ToProtoSubjectsTest {

    private ToProtoSubjects function;

    @BeforeEach
    void newFunction() {
        function = new ToProtoSubjects();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(function);
    }

    @Test
    @DisplayName("transform received entity states into proto subjects")
    void transformEntityStates() {
        SubscriptionUpdate update = GivenSubscriptionUpdate.withTwoEntities();
        Iterable<ProtoSubject> subjects = function.apply(update);

        assertThat(subjects).hasSize(2);

        ProtoSubject assertFirstItem = subjects.iterator()
                                                           .next();
        assertFirstItem.isInstanceOf(BbProject.class);
        ProtoSubject assertSecondItem = subjects.iterator()
                                                            .next();
        assertSecondItem.isInstanceOf(BbProject.class);
    }

    @Test
    @DisplayName("transform received events into proto subjects")
    void transformEvents() {
        SubscriptionUpdate update = GivenSubscriptionUpdate.withTwoEvents();
        Iterable<ProtoSubject> subjects = function.apply(update);

        assertThat(subjects).hasSize(2);

        ProtoSubject assertFirstItem = subjects.iterator()
                                                           .next();
        assertFirstItem.isInstanceOf(BbProjectCreated.class);
        ProtoSubject assertSecondItem = subjects.iterator()
                                                            .next();
        assertSecondItem.isInstanceOf(BbProjectCreated.class);
    }

    @Test
    @DisplayName("return empty collection if update list is empty")
    void transformToEmpty() {
        SubscriptionUpdate update = GivenSubscriptionUpdate.empty();
        Iterable<ProtoSubject> subjects = function.apply(update);

        assertThat(subjects).isEmpty();
    }
}
