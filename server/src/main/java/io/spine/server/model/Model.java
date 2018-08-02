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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.spine.annotation.Internal;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.reflect.PackageInfo;
import io.spine.server.annotation.BoundedContext;
import io.spine.server.security.InvocationGuard;

import java.util.Map;
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

    /**
     * Maps a raw class of a model object to corresponding Model instance.
     */
    private static final Map<Class<?>, Model> models = Maps.newConcurrentMap();

    /** The name of the Bounded Context to which this instance belongs. */
    private final BoundedContextName context;

    /** Maps a raw Java class to a {@code ModelClass}. */
    private final ClassMap classes = new ClassMap();

    /**
     * Creates a new instance for the Bounded Context with the passed name. */
    private Model(BoundedContextName context) {
        this.context = context;
    }

    @SuppressWarnings("unused") // The param will be used when Model is created per BoundedContext.
    public static <T> Model getInstance(Class<? extends T> rawClass) {
        Model model = models.get(rawClass);
        if (model != null) {
            return model;
        }

        Optional<BoundedContextName> optional = findContext(rawClass);

        // If no name of a Bounded Context found, assume the default name.
        // This is a safety net for newcomers and our tests.
        // We may want to make this check strict, and require specifying Bounded Context names.
        BoundedContextName context = optional.orElseGet(BoundedContextNames::assumingTests);

        // Try to find a Model if it already exists.
        Optional<Model> alreadyAvailable =
                models.values()
                      .stream()
                      .filter((m) -> m.context.equals(context))
                      .findAny();

        if (alreadyAvailable.isPresent()) {
            return alreadyAvailable.get();
        }

        // Since a model is not found, create for new Bounded Context and associated with the
        // passed raw class.
        Model newModel = new Model(context);
        models.put(rawClass, newModel);
        return newModel;
    }

    /**
     * Finds Bounded Context name in the package annotations of the raw class,
     * or in the packages into which the package of the class is nested.
     */
    @VisibleForTesting
    static <T> Optional<BoundedContextName> findContext(Class<? extends T> rawClass) {
        PackageInfo pkg = PackageInfo.of(rawClass);
        Optional<BoundedContext> annotation = pkg.findAnnotation(BoundedContext.class);
        if (!annotation.isPresent()) {
            return Optional.empty();
        }
        String contextName = annotation.get()
                                       .value();
        BoundedContextName result = BoundedContextNames.newName(contextName);
        return Optional.of(result);
    }

    /**
     * Clears the classes already added to the {@code Model}.
     *
     * <p>This method can be useful when multiple Spine projects are processed under the same
     * static context, e.g. in tests.
     */
    private void clear() {
        classes.clear();
    }

    /**
     * Clears all models, and then clears the {@link #models} map.
     */
    private static void reset() {
        for (Model model : models.values()) {
            model.clear();
        }
        models.clear();
    }

    /**
     * Clears all models and removes them.
     *
     * <p>This method must <em>not</em> be called from the production code.
     *
     * @apiNote This method <em>may</em> be called indirectly from authorized tool classes
     *          or test utility classes.
     * @throws SecurityException if called directly by non-authorized class
     */
    @VisibleForTesting
    public static synchronized void dropAllModels() {
        InvocationGuard.allowOnly(
                "io.spine.server.model.ModelTest",
                "io.spine.testing.server.model.ModelTests",
                "io.spine.model.verify.ModelVerifier"
        );
        reset();
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
}
