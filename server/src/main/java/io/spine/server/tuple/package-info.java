/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
 * This package provides tuples for return values of command handling methods.
 *
 * <p>Although tuples are <a href="https://github.com/google/guava/wiki/IdeaGraveyard#tuples-for-n--2">
 * considered harmful</a> in general, there is a valid case of their usage when there is a need for
 * returning more than one event message from a command handling method.
 *
 * <p>For example, the return value of the below method does not say much about the number and types
 * of returned event messages.
 * <pre>{@code
 *     {@literal @}Assign
 *     List<Message> on(CreateTask cmd) { ... }
 * }</pre>
 *
 * The below declaration gives both number and types of events:
 * <pre>{@code
 *     {@literal @}Assign
 *     Pair<TaskCreated, TaskAssigned> on(CreateTask cmd) { ... }
 * }</pre>
 *
 * <p>The following tuple classes are provided:
 * <ul>
 *    <li>{@code Pair<A, B>}
 *    <li>{@code Triplet<A, B, C>}
 *    <li>{@code Quartet<A, B, C, D>}
 *    <li>{@code Quintet<A, B, C, D, E>}
 *    <li>{@code Sextet<A, B, C, D, E, F>}
 *    <li>{@code Septet<A, B, C, D, E, F, G>}
 *    <li>{@code Octet<A, B, C, D, E, F, G, H>}
 *    <li>{@code Ennead<A, B, C, D, E, F, G, H, I>}
 *    <li>{@code Decade<A, B, C, D, E, F, G, H, I, J>}
 * </ul>
 *
 * <p>Each of the generic type is a specific class derived from
 * {@link com.google.protobuf.Message Message}.
 *
 * <p>In order to define alternatively returned values, please use the following classes:
 * <ul>
 *     <li>{@code Either<A, B>}
 *     <li>{@code EitherOfThree<A, B, C>}
 *     <li>{@code EitherOfFour<A, B, C, D>}
 *     <li>{@code EitherOfFive<A, B, C, D, E>}
 * </ul>
 *
 * <p>We believe that a list of alternatives longer than five is hard to understand.
 * If you face a such a need, consider to spliting a command into two or more so that their outcome
 * is more obvious.
 */

@ParametersAreNonnullByDefault
package io.spine.server.tuple;

import javax.annotation.ParametersAreNonnullByDefault;
