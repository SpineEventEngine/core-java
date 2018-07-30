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

package io.spine.server.model;

import io.spine.value.ClassTypeValue;

import java.util.function.Supplier;

/**
 * A class that belongs to a {@link Model}.
 *
 * @param <T> the type of objects
 * @author Alexander Yevsyukov
 */
public abstract class ModelClass<T> extends ClassTypeValue<T> {

    private static final long serialVersionUID = 0L;

    protected ModelClass(Class<? extends T> value) {
        super(value);
    }

    /**
     * Obtains the model class for the passed raw class.
     *
     * <p>If the model does not have the model class yet, it would be obtained
     * from the passed supplier and remembered.
     */
    public static <T, M extends ModelClass>
    ModelClass<T> get(Class<T> rawClass,
                      Class<M> requestedModelClass,
                      Supplier<ModelClass<T>> supplier) {
        Model model = Model.getInstance(rawClass);
        ModelClass<T> result = model.getClass(rawClass, requestedModelClass, supplier);
        return result;
    }
}
