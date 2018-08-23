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

package io.spine.server.aggregate;

import io.spine.server.aggregate.given.importado.DotSpace;
import io.spine.server.aggregate.given.importado.ObjectId;
import io.spine.server.aggregate.given.importado.event.Moved;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.given.importado.Direction.EAST;
import static io.spine.server.aggregate.given.importado.Direction.NORTH;
import static io.spine.server.aggregate.given.importado.MoveMessages.move;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;

/**
 * This class contains test for importing events into aggregate via {@link Apply}
 * methods that {@linkplain Apply#allowImport() allow import} of events.
 *
 * @author Alexander Yevsyukov
 */
@DisplayName("Aggregate which supports event import should")
class ApplyAllowImportTest {

    /**
     * Black-box test that ensures that the aggregate works in a normal way.
     */
    @Test
    @DisplayName("use event appliers in a traditional way")
    void normalApply() {
        ObjectId id = ObjectId.newBuilder()
                              .setValue("Луноход-1")
                              .build();

        BlackBoxBoundedContext
                .newInstance()
                .with(new DotSpace())
                .receivesCommands(move(id, NORTH), move(id, EAST))
                .assertThat(emittedEvent(Moved.class, twice()))
                .close();
    }
}
