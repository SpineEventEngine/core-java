/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.server.type;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.protobuf.Message;
import io.spine.base.RejectionMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.type.MessageClass;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.core.Events.ensureMessage;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A value object holding a class of a business rejection.
 */
public final class RejectionClass extends MessageClass<RejectionMessage> {

    private static final long serialVersionUID = 0L;

    private RejectionClass(Class<? extends RejectionMessage> value) {
        super(value);
    }

    /**
     * Creates a new instance of the rejection class.
     *
     * @param value
     *         a value to hold
     * @return new instance
     */
    public static RejectionClass of(Class<? extends RejectionMessage> value) {
        return new RejectionClass(checkNotNull(value));
    }

    /**
     * Creates a new instance of the rejection class by passed rejection instance.
     *
     * <p>If an instance of {@link Event} which implements {@code Message} is
     * passed to this method, enclosing rejection message will be un-wrapped to determine
     * the class of the rejection.
     *
     * @param rejectionOrMessage
     *         a rejection instance
     * @return new instance
     */
    public static RejectionClass of(Message rejectionOrMessage) {
        RejectionMessage message = (RejectionMessage) ensureMessage(rejectionOrMessage);
        RejectionClass result = of(message.getClass());
        return result;
    }

    /**
     * Creates a new instance from the given {@code ThrowableMessage}.
     */
    public static RejectionClass of(ThrowableMessage rejection) {
        RejectionMessage rejectionMessage = rejection.messageThrown();
        return of(rejectionMessage);
    }

    /**
     * Obtains the class of a rejection by the class of corresponding throwable message.
     *
     * <p>This method assumes that outer classes containing rejections have the name ending with
     * {@code "Rejections"}. If this convention is not followed this method will fail.
     *
     * @throws IllegalArgumentException
     *         if there is no matching rejection class found under outer classes in the same
     *         package with the passed class
     */
    public static RejectionClass from(Class<? extends ThrowableMessage> cls) {
        Locator locator = new Locator(cls);
        RejectionClass result =
                locator.find()
                       .map(RejectionClass::of)
                       .orElseThrow(
                               () -> newIllegalArgumentException(
                                       "Unable to find a rejection class matching the class `%s`",
                                       cls.getName())
                       );
        return result;
    }

    /**
     * Finds a {@link RejectionMessage} class matching the passed class of {@link ThrowableMessage}.
     *
     * @implNote This class assumes that the user does not overwrite the convention
     *         using the {@code java_outer_classname} option of the proto file or when overwriting
     *         follows the convention of having the {@link #REJECTION_OUTER_CLASS_SUFFIX}.
     *         If the user does not follow the convention, rejections will still be generated,
     *         but this class won't be able to find them.
     *
     * @see <a href="https://github.com/SpineEventEngine/core-java/issues/1192">The issue</a> on
     * the limitations of this class.
     */
    private static final class Locator {

        /**
         * The conventional suffix for outer classes containing rejection message classes.
         */
        private static final String REJECTION_OUTER_CLASS_SUFFIX = "Rejections";

        private final Class<? extends ThrowableMessage> throwableClass;
        private final ClassPath classPath;

        private Locator(Class<? extends ThrowableMessage> cls) {
            this.throwableClass = cls;
            this.classPath = classPath();
        }

        Optional<Class<? extends RejectionMessage>> find() {
            Package classPackage = throwableClass.getPackage();
            String packageName = classPackage.getName();
            ImmutableSet<ClassInfo> rejectionOuterClasses = rejectionOuterClasses(packageName);
            @SuppressWarnings("unchecked") /* The cast is ensures by the fact that generated
              rejection messages are placed inside the outer class which name ends
              with "Rejections". We also check that for the simple name equality, which is
              also a part of the convention. */
            Optional<Class<? extends RejectionMessage>> result =
                    rejectionOuterClasses
                            .stream()
                            .map(ClassInfo::load)
                            .flatMap(outerClass -> Arrays.stream(outerClass.getClasses()))
                            .filter(innerClass -> innerClass.getSimpleName()
                                                            .equals(throwableClass.getSimpleName()))
                            .findFirst()
                            .map(c -> (Class<? extends RejectionMessage>) c);
            return result;
        }

        private ImmutableSet<ClassInfo> rejectionOuterClasses(String packageName) {
            ImmutableSet<ClassInfo> topLevelClasses =
                    classPath.getTopLevelClasses(packageName);
            return topLevelClasses.stream()
                           .filter(c -> c.getName()
                                         .endsWith(REJECTION_OUTER_CLASS_SUFFIX))
                           .collect(toImmutableSet());
        }

        private ClassPath classPath() {
            try {
                return ClassPath.from(throwableClass.getClassLoader());
            } catch (IOException e) {
                throw illegalStateWithCauseOf(e);
            }
        }
    }
}
