/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.procman;

import org.spine3.server.internal.AbstractEntityId;

/**
 * A value object for process manager IDs.
 *
 * @param <I> the type of process manager IDs
 * @author Alexander Litus
 */
public class ProcessManagerId<I> extends AbstractEntityId<I> {

    /**
     * The standard name for properties holding an ID of a process manager.
     */
    public static final String PROPERTY_NAME = "processManagerId";

    /**
     * The standard name for a parameter containing a process manager ID.
     */
    public static final String PARAM_NAME = PROPERTY_NAME;

    private ProcessManagerId(I value) {
        super(value);
    }

    /**
     * Creates a new non-null ID of a process manager.
     *
     * @param value id value
     * @return new manager instance
     */
    public static <I> ProcessManagerId<I> of(I value) {
        return new ProcessManagerId<>(value);
    }
}
