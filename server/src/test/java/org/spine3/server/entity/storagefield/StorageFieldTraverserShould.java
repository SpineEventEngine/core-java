/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity.storagefield;

import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.StorageFields;
import org.spine3.test.entity.Project;
import org.spine3.testdata.Sample;

import javax.annotation.Nullable;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Dmytro Dashenkov.
 */
public class StorageFieldTraverserShould {

    @Test
    public void traverse_over_storage_fields() {
        final String intKey = "Int_value";
        final String longKey = "Long_value";
        final String stringKey = "String_value";
        final String boolKey = "Bool_value";
        final String floatKey = "Float_value";
        final String doubleKey = "Double_value";
        final String message1Key = "Message_value";
        final String message2Key = "Second_message_value";

        final int intValue = 42;
        final long longValue = 42L;
        final String stringValue = "some random string";
        final boolean boolValue = true;
        final float floatValue = 4.2f;
        final double doubleValue = 42.0;
        final Message messageValue = Sample.messageOfType(Project.class);

        final StorageFields fields =
                StorageFields.newBuilder()
                             .putIntegerField(intKey, intValue)
                             .putLongField(longKey, longValue)
                             .putBooleanField(boolKey, boolValue)
                             .putStringField(stringKey, stringValue)
                             .putFloatField(floatKey, floatValue)
                             .putDoubleField(doubleKey, doubleValue)
                             .putAnyField(message1Key, AnyPacker.pack(messageValue))
                             .putAnyField(message2Key, AnyPacker.pack(messageValue))
                             .build();
        final StorageFieldTraverser traverser = spy(new Traverser());
        traverser.traverse(fields);
        verify(traverser).hitInteger(eq(intKey), eq(intValue));
        verify(traverser).hitLong(eq(longKey), eq(longValue));
        verify(traverser).hitString(eq(stringKey), eq(stringValue));
        verify(traverser).hitBoolean(eq(boolKey), eq(boolValue));
        verify(traverser).hitFloat(eq(floatKey), eq(floatValue));
        verify(traverser).hitDouble(eq(doubleKey), eq(doubleValue));
        verify(traverser, times(2)).hitMessage(anyString(), eq(messageValue));
    }

    private static class Traverser extends StorageFieldTraverser {

        @Override
        protected void hitMessage(String fieldName, @Nullable Message value) {

        }

        @Override
        protected void hitInteger(String fieldName, @Nullable Integer value) {

        }

        @Override
        protected void hitLong(String fieldName, @Nullable Long value) {

        }

        @Override
        protected void hitString(String fieldName, @Nullable String value) {

        }

        @Override
        protected void hitBoolean(String fieldName, @Nullable Boolean value) {

        }

        @Override
        protected void hitFloat(String fieldName, @Nullable Float value) {

        }

        @Override
        protected void hitDouble(String fieldName, @Nullable Double value) {

        }
    }
}
