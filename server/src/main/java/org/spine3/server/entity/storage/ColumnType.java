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

package org.spine3.server.entity.storage;

import org.spine3.annotations.SPI;

/**
 * An interface for handling type conversion for the {@link Column Columns}.
 *
 * <p>When retrieved from an instance of an {@link org.spine3.server.entity.Entity Entity},
 * the {@link Column Column} may be of an inappropriate type for storing. To convert the value into
 * an appropriate type, use {@link #convertColumnValue}.
 *
 * <p>After the conversion, you might want to save the value into a special storage specific DTO,
 * which commonly is built in a key-value fashion. To do that, call {@link #setColumnValue} and pass
 * both the DTO to store the value to and the key to store it under.
 *
 * <p>Example of implementing this interface for a JDBC-based storage:
 * <code>
 *     <pre>
 *         class VarcharDateType implements ColumnType<Date, String, PreparedStatement, Integer> {
 *             \@Override
 *             public String convert(Date fieldValue) {
 *                 return MY_DATE_FORMAT.format(fieldValue);
 *             }
 *
 *             \@Override
 *             public void set(PreparedStatement storageRecord, String value, Integer columnIdentifier) {
 *                 storageRecord.setString(columnIdentifier, value);
 *             }
 *         }
 *     </pre>
 * </code>
 *
 * <p>The example above translates a {@linkplain java.util.Date} into a formatted {@code String},
 * which is persisted into the DB.
 *
 * <p>It's necessary to make these operations atomic to allow automatic type conversion when
 * performing the DB queries.
 *
 * @param <J> the Java type represented by the column
 * @param <S> the "store as" type of the column
 * @param <R> the type of the record in the database, which holds a single cortege of data and
 *            is consumed by the database upon write
 * @param <C> the type of the column identifier in the {@code R}
 */
@SPI
public interface ColumnType<J, S, R, C> {

    /**
     * Converts the {@link Column Column} specified in
     * the {@link org.spine3.server.entity.Entity Entity} declaration to the type in which the Field
     * is stored.
     *
     * <p>Common example is converting
     * {@link com.google.protobuf.Timestamp com.google.protobuf.Timestamp} into
     * {@link java.util.Date java.util.Date}.
     *
     * @param fieldValue the {@link Column Column} of the initial type
     * @return the {@link Column Column} of the "store as" type
     */
    S convertColumnValue(J fieldValue);

    /**
     * Set the {@link Column Column} value to the database record type.
     *
     * <p>Common example is setting a value to
     * a {@link java.sql.PreparedStatement PreparedStatement} instance into a determined position.
     *
     * @param storageRecord    the database record
     * @param value            the value to store
     * @param columnIdentifier the identifier of the column, e.g. its index
     */
    void setColumnValue(R storageRecord, S value, C columnIdentifier);
}
