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

package io.spine.testing.server.procman;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.protobuf.Message;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.entity.EntitySubject;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Assertions for process managers.
 *
 * @param <S> the type of the process manager state
 * @param <P> the type of the process manager
 */
public final class PmSubject<S extends Message, P extends ProcessManager<?, S, ?>>
        extends EntitySubject<PmSubject<S, P>, S, P> {

    private PmSubject(FailureMetadata metadata, @NullableDecl P actual) {
        super(metadata, actual);
    }

    public static <S extends Message, P extends ProcessManager<?, S, ?>>
    Subject.Factory<PmSubject<S, P>, P> processManagers() {
        return PmSubject::new;
    }
}
