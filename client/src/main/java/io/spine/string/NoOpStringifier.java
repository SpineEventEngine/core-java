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

/**
 * The {@code Stringifier} for the {@code String} values.
 *
 * <p>Always returns the original {@code String} passed as an argument.
 *
 * @author Illia Shepilov
 * @author Alexander Yevsyukov
 */
final class NoOpStringifier extends Stringifier<String> implements Serializable {

    private static final long serialVersionUID = 0L;

    private static final NoOpStringifier INSTANCE = new NoOpStringifier();

    static NoOpStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(String obj) {
        return obj;
    }

    @Override
    protected String fromString(String s) {
        return s;
    }

    @Override
    public String toString() {
        return "Stringifiers.forString()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
