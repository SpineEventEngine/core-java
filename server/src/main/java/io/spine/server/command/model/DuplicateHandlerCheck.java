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

package io.spine.server.command.model;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.model.Model;
import io.spine.server.model.ModelClass;
import io.spine.server.procman.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.server.command.model.CommandHandlerClass.asCommandHandlerClass;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Ensures that there are no duplicating command handling methods among passed classes.
 *
 * @author Alexander Yevsyukov
 */
public class DuplicateHandlerCheck {

    /**
     * Maps a raw class to a function that creates appropriate model class, which in turn
     * appends itself to corresponding model.
     */
    @SuppressWarnings({
            "unchecked", // The cast is preserved by correct key-value pairs.
            "CheckReturnValue" /* Returned values for asXxxClass() are ignored because we use
                                  these methods only for verification of the classes. */
    })
    private final ImmutableMap<Class<?>, Appender> appenders =
            ImmutableMap.<Class<?>, Appender>builder()
                    .put(Aggregate.class,
                         (c) -> asAggregateClass((Class<? extends Aggregate>) c))
                    .put(ProcessManager.class,
                         (c) -> asProcessManagerClass((Class<? extends ProcessManager>) c))
                    .put(AbstractCommandHandler.class,
                         (c) -> asCommandHandlerClass((Class<? extends AbstractCommandHandler>) c))

                    //TODO:2018-08-03:alexander.yevsyukov: Add CommanderClass here.

                    .build();

    private DuplicateHandlerCheck() {
    }

    /**
     * Creates a new instance of the check.
     */
    public static DuplicateHandlerCheck newInstance() {
        return new DuplicateHandlerCheck();
    }

    /**
     * Performs the check for the passed classes.
     */
    public void check(Iterable<Class<?>> classes) {
        log().debug("Dropping models...");
        Model.dropAllModels();
        for (Class<?> cls : classes) {
            add(cls);
        }
    }

    /**
     * Verifies if the passed raw Java class is accepted by
     * {@link Model Model} as a valid command handling class.
     *
     * <p>This means that commands handled by this class are not handled by other classes already
     * known to the Model.
     */
    public void add(Class<?> cls) {
        Logger log = log();
        for (Class<?> keyClass : appenders.keySet()) {
            if (keyClass.isAssignableFrom(cls)) {
                Appender appender = appenders.get(keyClass);
                appender.apply(cls);
                log.debug("`{}` added to the Model");
                return;
            }
        }

        throw newIllegalArgumentException(
                "Class %s is not a command handling type.", cls.getName()
        );
    }

    /**
     * Creates a model class by a raw class, and adds it to the Model.
     */
    @FunctionalInterface
    private interface Appender extends Function<Class<?>, ModelClass<?>> {

        @Override
        @CanIgnoreReturnValue
        ModelClass<?> apply(Class<?> aClass);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Model.class);
    }
}
