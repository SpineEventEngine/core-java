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

package io.spine.model.assemble.given;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * Memoizes the content of a given message and its {@linkplain Diagnostic.Kind kind}.
 */
public final class MemoizingMessager implements Messager {

    private final List<MemoizedMessage> messages = newLinkedList();

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        addMessage(kind, msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
        addMessage(kind, msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e,
                             AnnotationMirror a) {
        addMessage(kind, msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a,
                             AnnotationValue v) {
        addMessage(kind, msg);
    }

    public MemoizedMessage firstMessage() {
        if (messages.isEmpty()) {
            throw new IllegalStateException("No messages have been received yet.");
        }
        return messages.get(0);
    }

    private void addMessage(Diagnostic.Kind kind, CharSequence msg) {
        MemoizedMessage message = new MemoizedMessage(kind, msg);
        messages.add(message);
    }

    public static final class MemoizedMessage {
        private final Diagnostic.Kind kind;
        private final CharSequence message;

        private MemoizedMessage(Diagnostic.Kind kind, CharSequence message) {
            this.kind = kind;
            this.message = message;
        }

        public Diagnostic.Kind kind() {
            return kind;
        }

        public String messageAsString() {
            return message.toString();
        }
    }
}
