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

package io.spine.server.aggregate.given.repo;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.test.shared.StringAggregate;
import io.spine.server.test.shared.StringAggregateVBuilder;
import io.spine.test.aggregate.cli.Evaluate;
import io.spine.test.aggregate.cli.Evaluated;

import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;

/**
 * The aggregate that can handle status flags.
 *
 * <p>We use {@code StringValue} for messages to save on code generation
 * in the tests. Real aggregates should use generated messages.
 */
public class AggregateWithLifecycle
        extends Aggregate<Long, StringAggregate, StringAggregateVBuilder> {

    private AggregateWithLifecycle(Long id) {
        super(id);
    }

    @Assign
    Evaluated handle(Evaluate commandMessage) {
        String command = commandMessage.getCmd();
        String result = command + 'd';
        return Evaluated
                .newBuilder()
                .setCmd(result)
                .build();
    }

    @Apply
    void on(Evaluated eventMessage) {
        String msg = RepoOfAggregateWithLifecycle.getMessage(eventMessage);
        if (archived.name()
                    .equalsIgnoreCase(msg)) {
            setArchived(true);
        }
        if (deleted.name()
                   .equalsIgnoreCase(msg)) {
            setDeleted(true);
        }
        getBuilder().setValue(msg);
    }
}
