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
 * Test environment for testing the {@linkplain io.spine.testing.server.procman ProcessManager
 * testing utilities}.
 *
 * <p>Test suite classes in this package form the test environment for testing
 * abstract classes of {@linkplain io.spine.testing.server.procman ProcessManager tests}. These
 * classes use the {@link io.spine.testing.server.procman.given.pm pm} subpackage which contains
 * sample {@code ProcessManager} classes and corresponding repositories (that are subjects of
 * testing by sample test suite classes in this package).
 *
 * <p>The sample test suite classes do not have methods that run assertions. Assertion API of
 * abstract test classes is invoked by real tests placed under the
 * {@linkplain io.spine.testing.server.procman production code package}.
 *
 * @see io.spine.testing.server.procman.PmCommandOnEventTestShould
 * @see io.spine.testing.server.procman.PmCommandTestShould
 */

@CheckReturnValue
@ParametersAreNonnullByDefault
package io.spine.testing.server.procman.given;

import com.google.errorprone.annotations.CheckReturnValue;

import javax.annotation.ParametersAreNonnullByDefault;
