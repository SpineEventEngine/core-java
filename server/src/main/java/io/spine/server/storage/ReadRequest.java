/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.storage;

import io.spine.annotation.Internal;

/**
 * A read request for {@link Storage}.
 *
 * <p>Purpose of this interface is generalization of the parameter
 * for a record {@linkplain Storage#read(ReadRequest) reading}.
 *
 * <p>This generalization is required, because {@code ID} is not always sufficient parameter
 * to read a record. Implementations may specify other filtering parameters or even
 * configuration for a read execution. E.g. to read a record, reading of which causes
 * reading of a list of other records, a batch size will be handy and it can be specified as a
 * parameter of a read request.
 *
 * @param <I> the type of the record ID
 * @author Dmytro Grankin
 */
@Internal
public interface ReadRequest<I> {

    /**
     * Obtains the ID of the requested record.
     *
     * <p>The result of this request must have this ID.
     *
     * @return the record ID
     */
    I getId();
}
