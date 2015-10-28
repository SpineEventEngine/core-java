/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.filesystem;

import org.junit.Test;
import org.spine3.test.TestIdWithMultipleFields;
import org.spine3.test.TestIdWithStringField;
import org.spine3.util.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.spine3.server.storage.filesystem.FileSystemDepository.Identifiers.idToStringWithEscaping;

@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection"})
public class FileSystemHelperShould {

    // quotes escaped
    private static final String QUOT = "&#34;";

    protected static final String UTILITY_CLASS_SHOULD_HAVE_PRIVATE_CONSTRUCTOR = "Utility class should have private constructor";

    @Test
    @SuppressWarnings("OverlyBroadCatchBlock")
    public void have_private_constructor() {
        try {
            Tests.callPrivateUtilityConstructor(FileSystemDepository.class);
        } catch (Exception ignored) {
            fail(UTILITY_CLASS_SHOULD_HAVE_PRIVATE_CONSTRUCTOR);
        }
    }
    
    @Test
    @SuppressWarnings("LocalVariableNamingConvention")
    public void convert_id_to_string_and_escape_chars_not_allowed_in_file_name() {

        final String charsNotAllowedInFileName = "\\/:*?\"<>|";
        final String result = idToStringWithEscaping(charsNotAllowedInFileName);

        assertEquals("&#92;&#47;&#58;&#42;&#63;&#34;&#60;&#62;&#124;", result);
    }

    @Test
    public void convert_to_string_message_id_with_several_fields_and_escape_special_chars() {

        final String nestedString = "some_nested_string";
        final String outerString = "some_outer_string";
        final Integer number = 256;

        final TestIdWithMultipleFields idToConvert = TestIdWithMultipleFields.newBuilder()
                .setString(outerString)
                .setInt(number)
                .setMessage(TestIdWithStringField.newBuilder().setId(nestedString).build())
                .build();

        final String expected =
                "string=" + QUOT + outerString + QUOT +
                " int=" + number +
                " message { id=" + QUOT + nestedString + QUOT + " }";

        final String actual = idToStringWithEscaping(idToConvert);

        assertEquals(expected, actual);
    }

    //TODO:2015-09-28:mikhail.mikhaylov: How to test FileSystemHelper.checkConfigured()?
}
