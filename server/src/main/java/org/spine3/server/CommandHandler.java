/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import org.spine3.base.CommandContext;

/**
 * The marker interface for classes that expose command handling methods.
 *
 * <p>A command handler is responsible for:
 * <ol>
 *     <li>Changing the state of the business model</li>
 *     <li>Producing corresponding events.</li>
 * </ol>
 *
 * <p>Events are returned as values of command handling methods.
 *
 * <h2>Command handling methods</h2>
 * <p>A command handling method is a {@code public} method that accepts two parameters.
 * The first parameter is a command message. The second parameter is {@link CommandContext}.
 *
 * <p>The method returns an event message of the specific type, or {@code List} of messages
 * if it produces more than one event.
 *
 * <p>The method may throw one or more throwables derived from {@link FailureThrowable}.
 * Throwing a {@code FailureThrowable} indicates that the passed command cannot be handled
 * because of a business failure, which can be obtained via {@link FailureThrowable#getFailure()}.
 *
 * @author Alexander Yevsyukov
 * @see CommandDispatcher
 */
public interface CommandHandler {
}
