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

import io.spine.base.Environment;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
final class IdempotencyGuardSwitch implements Supplier<IdempotencyGuard> {

    private IdempotencyGuard value;

    /**
     * Prevents direct instantiation.
     */
    private IdempotencyGuardSwitch() {
    }

    static IdempotencyGuardSwitch newInstance() {
        return new IdempotencyGuardSwitch();
    }

    void init(HistoryLog log) {
        checkNotNull(log);
        this.value = IdempotencyGuard.lookingAt(log);
    }

    @Override
    public IdempotencyGuard get() {
        if (value != null) {
            return value;
        } else if (Environment.getInstance().isTests()) {
            value = IdempotencyGuard.lookingAt(DenyingHistory.INSTANCE);
            return value;
        } else {
            throw new IllegalStateException("IdempotencyGuard is not present.");
        }
    }
}
