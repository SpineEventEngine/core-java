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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.Version;

import static io.spine.core.Versions.checkIsIncrement;

/**
 * A setter of a new version for the entity in transaction.
 */
@Internal
public abstract class VersionIncrement {

    private final Transaction transaction;

    VersionIncrement(Transaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Advances the version of the entity in transaction.
     */
    void apply() {
        Version nextVersion = nextVersion();
        checkIsIncrement(transaction.getVersion(), nextVersion);
        transaction.setVersion(nextVersion);
    }

    /**
     * Returns the transaction on which the {@code VersionIncrement} operates.
     */
    Transaction transaction() {
        return transaction;
    }

    /**
     * Creates a new version for the entity in transaction.
     *
     * @return the incremented {@code Version} of the entity
     */
    protected abstract Version nextVersion();
}
