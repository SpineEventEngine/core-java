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

package io.spine.server.log;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LoggerConfig;
import com.google.common.testing.TestLogHandler;
import io.spine.core.UserId;
import io.spine.logging.Logging;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.log.given.Books;
import io.spine.server.log.given.CardAggregate;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.LogRecordSubject;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.server.log.given.Books.THE_HOBBIT;
import static io.spine.testing.logging.LogTruth.assertThat;
import static java.util.logging.Level.ALL;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

@MuteLogging
@DisplayName("`LoggingEntity` should")
class LoggingEntityTest {

    private FluentLogger logger;
    private TestLogHandler handler;
    private Handler[] defaultHandlers;
    private @Nullable Level defaultLevel;

    @BeforeEach
    void setUpLog() {
        logger = Logging.loggerFor(CardAggregate.class);
        handler = new TestLogHandler();
        LoggerConfig config = LoggerConfig.of(logger);
        defaultHandlers = config.getHandlers();
        for (Handler defaultHandler : defaultHandlers) {
            config.removeHandler(defaultHandler);
        }
        config.addHandler(handler);
        defaultLevel = config.getLevel();
        config.setLevel(ALL);
    }

    @AfterEach
    void resetLog() {
        LoggerConfig config = LoggerConfig.of(logger);
        config.removeHandler(handler);
        handler.close();
        for (Handler defaultHandler : defaultHandlers) {
            config.addHandler(defaultHandler);
        }
        config.setLevel(defaultLevel);
    }

    @Test
    @DisplayName("log handler method name and parameters")
    void includeSignalName() {
        UserId user = GivenUserId.generated();
        BorrowBooks command = borrowBooks(user);
        context().receivesCommand(command);
        List<LogRecord> records = handler.getStoredLogRecords();

        assertThat(records)
                .hasSize(2);

        LogRecordSubject assertFirstLog = assertThat(records.get(0));
        assertFirstLog.hasLevelThat()
                      .isEqualTo(FINE);
        assertFirstLog.hasMessageThat()
                      .contains(Books.implementingDdd()
                                     .getTitle());
        assertFirstLog.hasMethodNameThat()
                      .contains(command.getClass()
                                       .getSimpleName());

        LogRecordSubject assertSecondLog = assertThat(records.get(1));
        assertSecondLog.hasLevelThat()
                       .isEqualTo(FINE);
        assertSecondLog.hasMessageThat()
                       .contains(Books.domainDrivenDesign()
                                      .getTitle());
        assertSecondLog.hasMethodNameThat()
                       .contains(command.getClass()
                                        .getSimpleName());
    }

    @Test
    @DisplayName("log handler class name")
    void includeClassName() {
        UserId user = GivenUserId.generated();
        BorrowBooks command = borrowBooks(user);
        context().receivesCommand(command);
        List<LogRecord> records = handler.getStoredLogRecords();

        assertThat(records)
                .hasSize(2);
        for (LogRecord record : records) {
            assertThat(record).hasClassNameThat()
                              .isEqualTo(CardAggregate.class.getName());
        }
    }

    @Test
    @DisplayName("pass the throwable unchanged")
    void withCause() {
        UserId user = GivenUserId.generated();
        ReturnBook command = ReturnBook
                .newBuilder()
                .setCard(cardId(user))
                .setBook(THE_HOBBIT)
                .vBuild();
        context().receivesCommand(command);
        List<LogRecord> records = handler.getStoredLogRecords();
        assertThat(records)
                .hasSize(1);
        LogRecord record = records.get(0);
        LogRecordSubject assertRecord = assertThat(record);
        assertRecord.isError();
        assertRecord.hasThrowableThat()
                    .isInstanceOf(UnknownBook.class);
    }

    private static BlackBoxContext context() {
        return BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(CardAggregate.class)
        );
    }

    private static BorrowBooks borrowBooks(UserId reader) {
        LibraryCardId id = cardId(reader);
        BorrowBooks command = BorrowBooks
                .newBuilder()
                .setCard(id)
                .addBookId(Books.BIG_RED_BOOK)
                .addBookId(Books.BIG_BLUE_BOOK)
                .vBuild();
        return command;
    }

    private static LibraryCardId cardId(UserId reader) {
        return LibraryCardId
                    .newBuilder()
                    .setReader(reader)
                    .build();
    }

    @Nested
    @DisplayName("support method")
    class Support {

        private String message;

        @BeforeEach
        void randomizeMessage() {
            message = newUuid();
        }

        @Test
        @DisplayName("_severe")
        void severe() {
            testLevel(Logging::_severe, SEVERE);
        }

        @Test
        @DisplayName("_warn")
        void warn() {
            testLevel(Logging::_warn, WARNING);
        }

        @Test
        @DisplayName("_info")
        void info() {
            testLevel(Logging::_info, INFO);
        }

        @Test
        @DisplayName("_config")
        void config() {
            testLevel(Logging::_config, CONFIG);
        }

        @Test
        @DisplayName("_fine")
        void fine() {
            testLevel(Logging::_fine, FINE);
        }

        @Test
        @DisplayName("_finer")
        void finer() {
            testLevel(Logging::_finer, FINER);
        }

        @Test
        @DisplayName("_finest")
        void finest() {
            testLevel(Logging::_finest, FINEST);
        }

        @Test
        @DisplayName("_error")
        void error() {
            testLevel(Logging::_error, Logging.errorLevel());
        }

        @Test
        @DisplayName("_debug")
        void debug() {
            testLevel(Logging::_debug, Logging.debugLevel());
        }

        @Test
        @DisplayName("_trace")
        void trace() {
            testLevel(Logging::_trace, FINEST);
        }

        private void testLevel(Function<Logging, FluentLogger.Api> underscoreFunc,
                               Level expectedLevel) {
            CardAggregate aggregate = new CardAggregate();
            underscoreFunc.apply(aggregate).log(message);
            List<LogRecord> records = handler.getStoredLogRecords();
            assertThat(records)
                    .hasSize(1);
            LogRecordSubject assertLog = assertThat(records.get(0));
            assertLog.hasLevelThat()
                     .isEqualTo(expectedLevel);
            assertLog.hasMessageThat()
                     .isEqualTo(message);
        }
    }
}