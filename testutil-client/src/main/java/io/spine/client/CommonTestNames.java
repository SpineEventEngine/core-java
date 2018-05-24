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

package io.spine.client;

//TODO:2018-05-24:dmytro.kuzmin: Move to the base.testlib
/**
 * A storage for the common {@linkplain org.junit.jupiter.api.DisplayName test names}.
 *
 * <p>This class can be used to avoid string literal duplication when assigning
 * {@link org.junit.jupiter.api.DisplayName} to the common {@linkplain org.junit.jupiter.api.Test
 * test cases}.
 *
 * @author Dmytro Kuzmin
 */
public class CommonTestNames {

    /**
     * Prevents instantiation of this class.
     */
    private CommonTestNames() {
    }

    /**
     * A name for the test cases checking that a class has private parameterless (aka "utility")
     * constructor.
     */
    public static final String UTILITY_CTOR = "have private parameterless constructor";

    /**
     * A name for test cases checking that the class public methods do not accept {@code null} for
     * their non-{@linkplain javax.annotation.Nullable nullable} arguments.
     */
    public static final String NULL_TOLERANCE =
            "not accept nulls for non-Nullable public method arguments";
}
