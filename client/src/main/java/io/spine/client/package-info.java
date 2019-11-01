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
 * This package provides classes and interfaces for performing client requests like
 * posting commands, running queries, or creating subscriptions.
 *
 * <p>The term <em>actor</em> used in this API means the user on behalf of whom requests
 * are created and executed. The package provides two levels of API.
 *
 * <dl>
 *   <dt><strong>High-level API</strong></dt>
 *   <dd>is meant for client-side Java applications that would communicate with backend
 *   services via a gRPC connection. An instance of the {@link io.spine.client.Client} class
 *   establishes this connection and serves as a gateway for creating and posting requests.
 *   </dd>
 *
 *   <dt><strong>Low-level API</strong></dt>
 *   <dd>is meant for server-side code which needs to speak to backend services without involving
 *   gRPC connection. This API is also by the High-level API implementation.
 *   The entry point for creating client requests at this level is
 *   {@link io.spine.client.ActorRequestFactory}.</dd>
 * </dl>
 */

@CheckReturnValue
@ParametersAreNonnullByDefault
package io.spine.client;

import com.google.errorprone.annotations.CheckReturnValue;

import javax.annotation.ParametersAreNonnullByDefault;
