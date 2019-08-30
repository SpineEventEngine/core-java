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

package io.spine.server.delivery;

import com.google.common.testing.NullPointerTester;
import io.spine.test.delivery.Calc;
import io.spine.testing.UtilityClassTest;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the {@link InboxIds} utility.
 */
@DisplayName("`InboxIds` should")
class InboxIdsTest extends UtilityClassTest<InboxIds> {

    InboxIdsTest() {
        super(InboxIds.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(InboxId.class, InboxId.getDefaultInstance())
              .setDefault(TypeUrl.class, TypeUrl.of(Calc.class));
    }

    @Test
    @DisplayName("not accept nulls in package-private static methods if the arg is non-Nullable")
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        /* This test does assert via `NullPointerTester. */
    void nullCheckPublicStaticMethods() {
        NullPointerTester tester = new NullPointerTester();
        configure(tester);
        tester.testStaticMethods(getUtilityClass(), NullPointerTester.Visibility.PACKAGE);
    }
}
