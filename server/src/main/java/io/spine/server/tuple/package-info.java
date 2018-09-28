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

/**
 * This package provides tuples for return values from
 * {@linkplain io.spine.server.command.Assign command handling} or
 * {@linkplain io.spine.server.command.Command commanding} methods.
 *
 * <p>Although tuples are <a href="https://github.com/google/guava/wiki/IdeaGraveyard#tuples-for-n--2">
 * considered harmful</a> in general, they are useful for describing types of several messages
 * returned by a method. Consider the following example.
 *
 * <p>The return value of the below method does not say much about the number and types
 * of returned event messages.
 * <pre>{@code
 *     {@literal @}Assign
 *     List<Message> on(CreateTask cmd) { ... }
 * }</pre>
 *
 * The below declaration gives both number and types of the events:
 * <pre>{@code
 *     {@literal @}Assign
 *     Pair<TaskCreated, TaskAssigned> on(CreateTask cmd) { ... }
 * }</pre>
 *
 * <p>It should re-iterated that the purpose of this package is limited to the scenarios
 * described above. Programmers are strongly discouraged from applying tuples for other purposes.
 *
 * <h1>Generic Types</h1>
 *
 * <p>Classes provided by this package can support up to 5 generic parameters. They are named from
 * {@code <A>} through {@code <E>}.
 *
 * <p>The first generic parameter {@code <A>} must always be a specific
 * {@link com.google.protobuf.Message Message} class.
 *
 * <p>Types from {@code <B>} through {@code <E>} can be either {@code Message} or
 * {@link java.util.Optional Optional}. See sections below for details.
 *
 * <h1>Basic Tuples</h1>
 *
 * <p>The following tuple classes are provided:
 * <ul>
 *    <li>{@link io.spine.server.tuple.Pair Pair&lt;A, B&gt;}
 *    <li>{@link io.spine.server.tuple.Triplet Triplet&lt;A, B, C&gt;}
 *    <li>{@link io.spine.server.tuple.Quartet Quartet&lt;A, B, C, D&gt;}
 *    <li>{@link io.spine.server.tuple.Quintet Quintet&lt;A, B, C, D, E&gt;}
 * </ul>
 *
 * <p>Basic tuple classes allow {@link java.util.Optional Optional} starting from
 * the second generic argument.
 *
 * <h1>Alternatives</h1>
 *
 * <p>In order to define alternatively returned values, please use the following classes:
 * <ul>
 *     <li>{@link io.spine.server.tuple.EitherOf2 EitherOfTwo&lt;A, B&gt;}
 *     <li>{@link io.spine.server.tuple.EitherOf3 EitherOfThree&lt;A, B, C&gt;}
 *     <li>{@link io.spine.server.tuple.EitherOf4 EitherOfFour&lt;A, B, C, D&gt;}
 *     <li>{@link io.spine.server.tuple.EitherOf5 EitherOfFive&lt;A, B, C, D, E&gt;}
 * </ul>
 *
 * <p>Generic parameters for alternatives can be only {@link com.google.protobuf.Message Message}.
 *
 * <p>We believe that a list of alternatives longer than five is hard to understand.
 * If you face a such a need, consider splitting a command into two or more independent commands
 * so that their outcome is more obvious.
 *
 * <h1>Using Tuples with Alternatives</h1>
 *
 * <p>A {@link io.spine.server.tuple.Pair Pair} can be defined with the second parameter being on
 * of the {@link io.spine.server.tuple.Either Either} subclasses, and created using
 * {@link io.spine.server.tuple.Pair#withEither(com.google.protobuf.Message,
 * io.spine.server.tuple.Either) Pair.withEither()}.
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package io.spine.server.tuple;

import com.google.errorprone.annotations.CheckReturnValue;

import javax.annotation.ParametersAreNonnullByDefault;
