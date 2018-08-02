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

package io.spine.server.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a package as one belonging to a Bounded Context with
 * the {@linkplain #value() specified name}.
 *
 * <p>Java does not have the notion of package nesting. Packages in Java are separated namespaces,
 * which seem hierarchical for convenience.
 *
 * <h3>Nesting Convention</h3>
 * <p>This annotation assumes that nesting formed by a programmer when naming packages, <em>is</em>
 * a hierarchy. Terms mentioned below assume that package “inheritance” or “hierarchy” is a
 * convention supported by this framework, and not a standard feature of Java.
 *
 * <h3>Inheriting Package Annotation</h3>
 * <p>Packages which names start with the name of the annotated package “inherit” the annotation.
 * This means that these packages belong to the same Bounded Context <em>unless</em> they are
 * annotated with another Bounded Context name.
 *
 * <p>A possible usage scenario would be to have an aggregate root class placed in a “parent”
 * package, and aggregate parts being under “sub-packages”.
 *
 * <p>Packages that do not have a common “parent”, but annotated with the same name of a
 * Bounded Context, belong to the same Bounded Context.
 *
 * @author Alexander Yevsyukov
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BoundedContext {

    /**
     * The name of the Bounded Context to which the package belongs.
     */
    String value();
}
