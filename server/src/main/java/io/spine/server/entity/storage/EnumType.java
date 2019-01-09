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

package io.spine.server.entity.storage;

/**
 * Enumeration of persistence methods for the {@linkplain Enumerated enumerated values}.
 *
 * <p>The methods define the form in which the {@link Enum} objects are persisted in the data
 * storage as well as the conversion function between the {@link Enum} value and the persistence
 * value.
 *
 * @author Dmytro Kuzmin
 * @see Enumerated
 */
public enum EnumType {

    /**
     * A persistence method which uses Java {@link Enum}'s {@linkplain Enum#ordinal() ordinal} to
     * convert the {@link Enum} into the {@link Integer} value to save in the storage.
     */
    ORDINAL,

    /**
     * A persistence method which uses {@link Enum}'s {@linkplain Enum#name() name} property to
     * store the {@link Enum} value in the form of Java {@link String}.
     */
    STRING
}
