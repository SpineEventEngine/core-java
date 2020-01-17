/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.change;

import com.google.protobuf.ByteString;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.change.ChangePreconditions.checkNewValueNotEmpty;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ChangePreconditions utility should")
class ChangePreconditionsTest extends UtilityClassTest<ChangePreconditions> {

    ChangePreconditionsTest() {
        super(ChangePreconditions.class);
    }

    @Test
    @DisplayName("not accept empty ByteString value")
    void failOnEmptyByteString() {
        ByteString str = ByteString.EMPTY;
        assertThrows(IllegalArgumentException.class, () -> checkNewValueNotEmpty(str));
    }

    @Test
    @DisplayName("not accept empty String value")
    void failOnEmptyString() {
        String str = "";
        assertThrows(IllegalArgumentException.class, () -> checkNewValueNotEmpty(str));
    }
}
