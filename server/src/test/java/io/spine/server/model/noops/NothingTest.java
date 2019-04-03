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

package io.spine.server.model.noops;

import io.spine.server.model.noops.given.ArchiverPm;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.model.noops.given.NoOpMessageTestEnv.archiveSingleFile;
import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;

@DisplayName("When Nothing event is emitted")
class NothingTest {

    @Test
    @DisplayName("the bus should not know")
    void notPost() {
        SingleTenantBlackBoxContext boundedContext = BlackBoxBoundedContext
                .singleTenant()
                .with(new ArchiverPm.Repository())
                .receivesCommand(archiveSingleFile());
        boundedContext.assertThat(emittedEvent(count(0)))
                      .close();
    }
}
