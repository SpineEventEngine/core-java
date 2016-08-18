/*
 *
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package org.spine3.server.storage;

import com.google.protobuf.Any;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.stand.Stand;

/**
 * Contract for {@link Stand} storage.
 *
 * <p>Stores the latest {@link org.spine3.server.aggregate.Aggregate} states. The state is passed as {@link Any} proto message
 * and stored by its {@link TypeUrl}.
 *
 * <p>Not more than one {@code Aggregate} state object is stored for a single {@code TypeUrl}.
 *
 * <p>Allows to access the states via {@link TypeUrl} its.
 *
 * @author Alex Tymchenko
 * @see Any#getTypeUrl()
 * @see Stand
 */
public abstract class StandStorage extends AbstractStorage<TypeUrl, Any> {

    protected StandStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Update the storage with the {@link org.spine3.server.aggregate.Aggregate} state.
     *
     * @param aggregateState the state of {@code Aggregate} to store.
     */
    public abstract void write(Any aggregateState);

}
