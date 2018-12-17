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

package io.spine.server.bc.given;

import io.spine.server.entity.Repository;

/** A pair of {@linkplain io.spine.server.entity.Repository repositories}. */
public class RepositoryPair {

    private final Repository<?, ?> firstRepository;
    private final Repository<?, ?> secondRepository;

    /** Constructs a pair from provided repositories. */
    public RepositoryPair(Repository<?, ?> firstRepository, Repository<?, ?> secondRepository) {
        this.firstRepository = firstRepository;
        this.secondRepository = secondRepository;
    }

    /** Returns the first value of this repository pair. */
    public Repository<?, ?> first() {
        return firstRepository;
    }

    /** Returns the second value of this repository pair. */
    public Repository<?, ?> second() {
        return secondRepository;
    }
}
