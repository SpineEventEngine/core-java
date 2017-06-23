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

package io.spine.string;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code Stringifier} for boolean values.
 *
 * @author Alexander Yevsyukov
 */
final class BooleanStringifier extends Stringifier<Boolean> implements Serializable {

    private static final long serialVersionUID = 0L;

    private static final BooleanStringifier INSTANCE = new BooleanStringifier();

    static BooleanStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(Boolean value) {
        checkNotNull(value);
        return value.toString();
    }

    @Override
    protected Boolean fromString(String s) {
        checkNotNull(s);
        final Boolean result = Boolean.parseBoolean(s);
        return result;
    }

    @Override
    public String toString() {
        return "Stringifiers.forBoolean()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
