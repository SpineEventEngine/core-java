/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.core.Version;
import io.spine.server.entity.EntityBuilder;
import io.spine.validate.ValidatingBuilder;

/**
 * Utility class for building test instances of {@code ProcessManager}.
 *
 * @param <P> the type of process managers
 * @param <I> the type of process manager identifier
 * @param <S> the type of the process manager state
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class ProcessManagerBuilder<P extends ProcessManager<I, S, B>,
                                   I,
                                   S extends Message,
                                   B extends ValidatingBuilder<S, ?>>
        extends EntityBuilder<P, I, S> {

    public ProcessManagerBuilder() {
        super();
        // Have the constructor for easier location of usages.
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") // fix IDEA bug
    @Override
    public ProcessManagerBuilder<P, I, S, B> setResultClass(Class<P> entityClass) {
        super.setResultClass(entityClass);
        return this;
    }

    @Override
    protected void setState(P result, S state, Version version) {
        PmTransaction.startWith(result, state, version).commit();
    }
}
