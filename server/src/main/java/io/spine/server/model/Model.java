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

import io.spine.annotation.Internal;
import io.spine.core.BoundedContextName;
import io.spine.reflect.PackageInfo;
import io.spine.server.annotation.BoundedContext;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Stores information of message handling classes.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
@Internal
public class Model {

    /** Maps a raw Java class to a {@code ModelClass}. */
    private final ClassMap classes = new ClassMap();

    public static Model getInstance() {
        return Singleton.INSTANCE.value;
    }

    @SuppressWarnings("unused") // The param will be used when Model is created per BoundedContext.
    public static <T> Model getInstance(Class<? extends T> rawClass) {
        //TODO:2018-07-25:alexander.yevsyukov: Find the model for the raw class using the
        // @BoundedContext("MyBoundedContext") annotation in the one of the parent packages
        // of the passed class.
        return getInstance();
    }

    /** Prevents instantiation from outside. */
    private Model() {
    }

    /**
     * Clears the classes already added to the {@code Model}.
     *
     * <p>This method can be useful when multiple Spine projects are processed under the same
     * static context, e.g. in tests.
     */
    public void clear() {
        classes.clear();
    }

    /**
     * Obtains the model class for the passed raw class.
     *
     * <p>If the model does not have the model class yet, it would be obtained
     * from the passed supplier and remembered.
     */
    <T, M extends ModelClass>
    ModelClass<T> getClass(Class<? extends T> cls,
                           Class<M> classOfModelClass,
                           Supplier<ModelClass<T>> supplier) {
        ModelClass<T> result = classes.get(cls, classOfModelClass, supplier);
        return result;
    }

    /**
     * Finds Bounded Context name in the package annotations of the raw class,
     * or in the packages into which the package of the class is nested.
     */
    private static <T> Optional<BoundedContextName> findContext(Class<? extends T> rawClass) {
        PackageInfo pkg = PackageInfo.of(rawClass);
        Optional<BoundedContext> annotation = pkg.findAnnotation(BoundedContext.class);
        if (!annotation.isPresent()) {
            return Optional.empty();
        }
        String contextName = annotation.get()
                                       .name();
        BoundedContextName result = BoundedContextName
                .newBuilder()
                .setValue(contextName)
                .build();
        return Optional.of(result);
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Model value = new Model();
    }
}
