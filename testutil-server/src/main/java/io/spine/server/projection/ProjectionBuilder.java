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

package io.spine.server.projection;

import com.google.protobuf.Message;
import io.spine.core.Version;
import io.spine.server.entity.EntityBuilder;
import io.spine.validate.ValidatingBuilder;

/**
 * Utility class for building test {@code Projection}s.
 *
 * @param <P> the type of the projection to build
 * @param <I> the type of projection IDs
 * @param <S> the type of the projection state
 *
 * @author Alexander Yevsyukov
 */
public class ProjectionBuilder<P extends Projection<I, S, B>,
                               I,
                               S extends Message,
                               B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends EntityBuilder<P, I, S> {

    public ProjectionBuilder() {
        super();
        // Have the constructor for easier location of usages.
    }

    @Override
    public ProjectionBuilder<P, I, S, B> setResultClass(Class<P> entityClass) {
        super.setResultClass(entityClass);
        return this;
    }

    @Override
    protected void setState(P result, S state, Version version) {
        ProjectionTransaction.startWith(result, state, version)
                             .commit();
    }
}
