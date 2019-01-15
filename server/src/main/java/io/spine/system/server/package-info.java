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

/**
 * Provides implementation of the System Bounded Context.
 *
 * @apiNote The package is annotated {@code @BoundedContext("_System")} to provide
 * entities of this package with one shared {@code Model}.
 *
 * <p>{@link io.spine.server.BoundedContext BoundedContext}s serving entities
 * of this package will be created with
 * {@linkplain io.spine.core.BoundedContextNames#system(io.spine.core.BoundedContextName)
 * synthetic names} created after names of parent {@code BoundedContext}s.
 */
@Internal
@BoundedContext("_System")
@CheckReturnValue
@ParametersAreNonnullByDefault
package io.spine.system.server;

import com.google.errorprone.annotations.CheckReturnValue;
import io.spine.annotation.Internal;
import io.spine.server.annotation.BoundedContext;

import javax.annotation.ParametersAreNonnullByDefault;
