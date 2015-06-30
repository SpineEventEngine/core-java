package org.spine3.util;
/* 
    Copyright (c) 2009, Orbitz World Wide
	All rights reserved.

	Redistribution and use in source and binary forms, with or without modification, 
	are permitted provided that the following conditions are met:

		* Redistributions of source code must retain the above copyright notice, 
		  this list of conditions and the following disclaimer.
		* Redistributions in binary form must reproduce the above copyright notice, 
		  this list of conditions and the following disclaimer in the documentation 
		  and/or other materials provided with the distribution.
		* Neither the name of the Orbitz World Wide nor the names of its contributors 
		  may be used to endorse or promote products derived from this software 
		  without specific prior written permission.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
	OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
	LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
	THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
	(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
	OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;

import java.io.IOException;
import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

/**
 * Provide ascii text parsing and formatting support for proto2 instances. The implementation
 * largely follows google/protobuf/text_format.cc.
 * <p>
 * (c) 2009-10 Orbitz World Wide. All Rights Reserved.
 *
 * NOTE: This is temporary solution until this functionality is not implemented in Protobuf.
 *
 * @author eliran.bivas@gmail.com Eliran Bivas
 * @author aantonov@orbitz.com Alex Antonov
 *         <p>
 *         Based on the original code by:
 * @author wenboz@google.com Wenbo Zhu
 * @author kenton@google.com Kenton Varda
 */
@SuppressWarnings({"UtilityClass", "MagicNumber", "HardcodedLineSeparator", "OverlyComplexMethod", "OverlyLongMethod", "OverlyComplexClass"})
public class JsonFormat {

    private JsonFormat() {
    }

    /**
     * Outputs a textual representation of the Protocol Message supplied into the parameter output.
     * (This representation is the new version of the classic "ProtocolPrinter" output from the
     * original Protocol Buffer system)
     */
    public static void print(Message message, Appendable output) throws IOException {
        JsonGenerator generator = new JsonGenerator(output);
        generator.print("{");
        print(message, generator);
        generator.print("}");
    }

    /**
     * Outputs a textual representation of {@code fields} to {@code output}.
     */
    public static void print(UnknownFieldSet fields, Appendable output) throws IOException {
        JsonGenerator generator = new JsonGenerator(output);
        generator.print("{");
        printUnknownFields(fields, generator);
        generator.print("}");
    }

    /**
     * Like {@code print()}, but writes directly to a {@code String} and returns it.
     */
    public static String printToString(Message message) {
        try {
            //noinspection StringBufferWithoutInitialCapacity
            StringBuilder text = new StringBuilder();
            print(message, text);
            return text.toString();
        } catch (IOException e) {
            throw new RuntimeException("Writing to a StringBuilder threw an IOException (should never happen).",
                    e);
        }
    }

    protected static void print(@SuppressWarnings("TypeMayBeWeakened") Message message,
                                JsonGenerator generator) throws IOException {

        for (Iterator<Map.Entry<FieldDescriptor, Object>> iter = message.getAllFields().entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<FieldDescriptor, Object> field = iter.next();
            printField(field.getKey(), field.getValue(), generator);
            if (iter.hasNext()) {
                generator.print(",");
            }
        }
        if (message.getUnknownFields().asMap().size() > 0)
            generator.print(", ");
        printUnknownFields(message.getUnknownFields(), generator);
    }

    public static void printField(FieldDescriptor field, Object value, JsonGenerator generator) throws IOException {

        printSingleField(field, value, generator);
    }

    private static void printSingleField(FieldDescriptor field,
                                         Object value,
                                         JsonGenerator generator) throws IOException {
        if (field.isExtension()) {
            generator.print("\"");
            // We special-case MessageSet elements for compatibility with proto1.
            boolean messageSetWireFormat = field.getContainingType().getOptions().getMessageSetWireFormat();
            boolean isMessage = FieldDescriptor.Type.MESSAGE == field.getType();
            boolean objectsAreEqual = Objects.equals(field.getExtensionScope(), field.getMessageType());

            @SuppressWarnings("OverlyComplexBooleanExpression") boolean matches = messageSetWireFormat && isMessage && (field.isOptional()) && objectsAreEqual;

            if (matches) {
                generator.print(field.getMessageType().getFullName());
            } else {
                generator.print(field.getFullName());
            }
            generator.print("\"");
        } else {
            generator.print("\"");
            if (field.getType() == FieldDescriptor.Type.GROUP) {
                // Groups must be serialized with their original capitalization.
                generator.print(field.getMessageType().getName());
            } else {
                generator.print(field.getName());
            }
            generator.print("\"");
        }

        // Done with the name, on to the value

        if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator.print(": ");
            generator.indent();
        } else {
            generator.print(": ");
        }


        if (field.isRepeated()) {
            // Repeated field. Print each element.
            generator.print("[");
            for (@SuppressWarnings("OverlyStrongTypeCast") Iterator<?> iter = ((List<?>) value).iterator(); iter.hasNext(); ) {
                printFieldValue(field, iter.next(), generator);
                if (iter.hasNext()) {
                    generator.print(",");
                }
            }
            generator.print("]");
        } else {
            printFieldValue(field, value, generator);
            if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
                generator.outdent();
            }
        }
    }

    private static void printFieldValue(FieldDescriptor field, Object value, JsonGenerator generator) throws IOException {
        switch (field.getType()) {
            case INT32:
            case INT64:
            case SINT32:
            case SINT64:
            case SFIXED32:
            case SFIXED64:
            case FLOAT:
            case DOUBLE:
            case BOOL:
                // Good old toString() does what we want for these types.
                generator.print(value.toString());
                break;

            case UINT32:
            case FIXED32:
                generator.print(unsignedToString((Integer) value));
                break;

            case UINT64:
            case FIXED64:
                generator.print(unsignedToString((Long) value));
                break;

            case STRING:
                generator.print("\"");
                generator.print(escapeText((String) value));
                generator.print("\"");
                break;

            case BYTES: {
                generator.print("\"");
                generator.print(escapeBytes((ByteString) value));
                generator.print("\"");
                break;
            }

            case ENUM: {
                generator.print("\"");
                //noinspection OverlyStrongTypeCast
                generator.print(((EnumValueDescriptor) value).getName());
                generator.print("\"");
                break;
            }

            case MESSAGE:
            case GROUP:
                generator.print("{");
                print((Message) value, generator);
                generator.print("}");
                break;
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    protected static void printUnknownFields(UnknownFieldSet unknownFields, JsonGenerator generator) throws IOException {
        boolean firstField = true;
        for (Map.Entry<Integer, UnknownFieldSet.Field> entry : unknownFields.asMap().entrySet()) {
            UnknownFieldSet.Field field = entry.getValue();

            if (firstField) {
                firstField = false;
            } else {
                generator.print(", ");
            }

            generator.print("\"");
            generator.print(entry.getKey().toString());
            generator.print("\"");
            generator.print(": [");

            boolean firstValue = true;
            for (long value : field.getVarintList()) {
                if (firstValue) {
                    firstValue = false;
                } else {
                    generator.print(", ");
                }
                generator.print(unsignedToString(value));
            }
            for (int value : field.getFixed32List()) {
                if (firstValue) {
                    firstValue = false;
                } else {
                    generator.print(", ");
                }
                generator.print(String.format((Locale) null, "0x%08x", value));
            }
            for (long value : field.getFixed64List()) {
                if (firstValue) {
                    firstValue = false;
                } else {
                    generator.print(", ");
                }
                generator.print(String.format((Locale) null, "0x%016x", value));
            }
            for (ByteString value : field.getLengthDelimitedList()) {
                if (firstValue) {
                    firstValue = false;
                } else {
                    generator.print(", ");
                }
                generator.print("\"");
                generator.print(escapeBytes(value));
                generator.print("\"");
            }
            for (UnknownFieldSet value : field.getGroupList()) {
                if (firstValue) {
                    firstValue = false;
                } else {
                    generator.print(", ");
                }
                generator.print("{");
                printUnknownFields(value, generator);
                generator.print("}");
            }
            generator.print("]");
        }
    }

    /**
     * Convert an unsigned 32-bit integer to a string.
     */
    @SuppressWarnings({"TypeMayBeWeakened", "OverloadedMethodsWithSameNumberOfParameters"})
    private static String unsignedToString(int value) {
        //noinspection MagicNumber
        return value >= 0
                ? Integer.toString(value)
                : Long.toString((value) & 0x00000000FFFFFFFFL);
    }

    /**
     * Convert an unsigned 64-bit integer to a string.
     */
    @SuppressWarnings({"TypeMayBeWeakened", "OverloadedMethodsWithSameNumberOfParameters"})
    private static String unsignedToString(long value) {
        //noinspection IfMayBeConditional
        if (value >= 0) {
            return Long.toString(value);
        } else {
            // Pull off the most-significant bit so that BigInteger doesn't think
            // the number is negative, then set it again using setBit().

            //noinspection MagicNumber
            return BigInteger.valueOf(value & 0x7FFFFFFFFFFFFFFFL).setBit(63).toString();
        }
    }

    // =================================================================
    // Utility functions
    //
    // Some of these methods are package-private because Descriptors.java uses
    // them.

    /**
     * Escapes bytes in the format used in protocol buffer text format, which is the same as the
     * format used for C string literals. All bytes that are not printable 7-bit ASCII characters
     * are escaped, as well as backslash, single-quote, and double-quote characters. Characters for
     * which no defined short-hand escape sequence is defined will be escaped using 3-digit octal
     * sequences.
     */
    static String escapeBytes(ByteString input) {
        StringBuilder builder = new StringBuilder(input.size());
        for (int i = 0; i < input.size(); i++) {
            byte b = input.byteAt(i);
            switch (b) {
                // Java does not recognize \a or \v, apparently.
                case 0x07:
                    builder.append("\\a");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case 0x0b:
                    builder.append("\\v");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\'':
                    builder.append("\\\'");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                default:
                    if (b >= 0x20) {
                        builder.append((char) b);
                    } else {
                        final String unicodeString = unicodeEscaped((char) b);
                        builder.append(unicodeString);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection", "ConfusingElseBranch"})
    static String unicodeEscaped(char ch) {
        if (ch < 0x10) {
            return "\\u000" + Integer.toHexString(ch);
        } else if (ch < 0x100) {
            return "\\u00" + Integer.toHexString(ch);
        } else if (ch < 0x1000) {
            return "\\u0" + Integer.toHexString(ch);
        }
        return "\\u" + Integer.toHexString(ch);
    }

    /**
     * Implements JSON string escaping as specified <a href="http://www.ietf.org/rfc/rfc4627.txt">here</a>.
     * <ul>
     * <li>The following characters are escaped by prefixing them with a '\' : \b,\f,\n,\r,\t,\,"</li>
     * <li>Other control characters in the range 0x0000-0x001F are escaped using the \\uXXXX notation</li>
     * <li>UTF-16 surrogate pairs are encoded using the \\uXXXX\\uXXXX notation</li>
     * <li>any other character is printed as-is</li>
     * </ul>
     */
    static String escapeText(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        CharacterIterator iter = new StringCharacterIterator(input);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            switch (c) {
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                default:
                    // Check for other control characters

                    // noinspection ConstantConditions
                    if (c >= 0x0000 && c <= 0x001F) {
                        appendEscapedUnicode(builder, c);
                    } else if (Character.isHighSurrogate(c)) {
                        // Encode the surrogate pair using 2 six-character sequence (\\uXXXX\\uXXXX)
                        appendEscapedUnicode(builder, c);
                        c = iter.next();
                        if (c == CharacterIterator.DONE)
                            throw new IllegalArgumentException("invalid unicode string: unexpected high surrogate pair value without corresponding low value.");
                        appendEscapedUnicode(builder, c);
                    } else {
                        // Anything else can be printed as-is
                        builder.append(c);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    static void appendEscapedUnicode(StringBuilder builder, char ch) {
        String prefix = "\\u";
        if (ch < 0x10) {
            prefix = "\\u000";
        } else if (ch < 0x100) {
            prefix = "\\u00";
        } else if (ch < 0x1000) {
            prefix = "\\u0";
        }
        builder.append(prefix).append(Integer.toHexString(ch));
    }

    /**
     * An inner class for writing text to the output stream.
     */
    @SuppressWarnings("PackageVisibleField")
    protected static class JsonGenerator {

        Appendable output;
        boolean atStartOfLine = true;
        @SuppressWarnings({"StringBufferWithoutInitialCapacity", "StringBufferField"})
        StringBuilder indent = new StringBuilder();

        public JsonGenerator(Appendable output) {
            this.output = output;
        }

        /**
         * Indent text by two spaces. After calling Indent(), two spaces will be inserted at the
         * beginning of each line of text. Indent() may be called multiple times to produce deeper
         * indents.
         */
        public void indent() {
            indent.append("  ");
        }

        /**
         * Reduces the current indent level by two spaces, or crashes if the indent level is zero.
         */
        public void outdent() {
            int length = indent.length();
            if (length == 0) {
                throw new IllegalArgumentException(" Outdent() without matching Indent().");
            }
            indent.delete(length - 2, length);
        }

        /**
         * Print text to the output stream.
         */
        public void print(CharSequence text) throws IOException {
            int size = text.length();
            int pos = 0;

            for (int i = 0; i < size; i++) {
                if (text.charAt(i) == '\n') {
                    write(text.subSequence(pos, size), i - pos + 1);
                    pos = i + 1;
                    atStartOfLine = true;
                }
            }
            write(text.subSequence(pos, size), size - pos);
        }

        private void write(CharSequence data, int size) throws IOException {
            if (size == 0) {
                return;
            }
            if (atStartOfLine) {
                atStartOfLine = false;
                output.append(indent);
            }
            output.append(data);
        }
    }

}
